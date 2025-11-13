package com.microservices.user_service.service;

import com.microservices.user_service.exception.DuplicateEmailException;
import com.microservices.user_service.exception.UnauthorizedException;
import com.microservices.user_service.exception.UserNotFoundException;
import com.microservices.user_service.mapper.UserMapper;
import com.microservices.user_service.model.Role;
import com.microservices.user_service.model.UserPrincipal;
import com.microservices.user_service.model.Users;
import com.microservices.user_service.model.dto.UserDto;
import com.microservices.user_service.model.dto.UserRegistrationDto;
import com.microservices.user_service.repository.UserRepository;
import com.microservices.user_service.service.impl.UserServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationDto registrationDto;
    private Users userEntity;
    private UserDto userDto;
    private UserPrincipal adminPrincipal;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setup() {
        registrationDto = UserRegistrationDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("12345")
                .address("Street")
                .phoneNumber("999")
                .build();

        userEntity = Users.builder()
                .userId("u2")
                .email("john@example.com")
                .roles(Set.of(Role.USER))
                .build();

        userDto = UserDto.builder()
                .userId("u1")
                .email("john@example.com")
                .roles(Set.of(Role.USER))
                .build();

        adminPrincipal = UserPrincipal.builder()
                .userId("admin")
                .email("a@a.com")
                .roles(Set.of(Role.SUPER_ADMIN))
                .build();

        userPrincipal = UserPrincipal.builder()
                .userId("u1")
                .email("john@example.com")
                .roles(Set.of(Role.USER))
                .build();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /* ------------ registerUser ------------ */

    @Test
    void registerUser_success() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("12345")).thenReturn("enc");
        when(userMapper.toEntity(registrationDto, "enc")).thenReturn(userEntity);
        when(userRepository.save(userEntity)).thenReturn(userEntity);
        when(userMapper.toDto(userEntity)).thenReturn(userDto);

        UserDto result = userService.registerUser(registrationDto);

        assertEquals("john@example.com", result.getEmail());
        verify(userRepository).save(userEntity);
    }

    @Test
    void registerUser_duplicateEmail() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        assertThrows(DuplicateEmailException.class, () -> userService.registerUser(registrationDto));
    }

    /* ------------ createSuperAdmin ------------ */

    @Test
    void createSuperAdmin_success() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(passwordEncoder.encode("12345")).thenReturn("enc");
        when(userRepository.save(any())).thenReturn(userEntity);
        when(userMapper.toDto(userEntity)).thenReturn(userDto);

        UserDto dto = userService.createSuperAdmin(registrationDto);

        assertEquals("john@example.com", dto.getEmail());
    }

    @Test
    void createSuperAdmin_duplicateEmail() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        assertThrows(DuplicateEmailException.class, () -> userService.createSuperAdmin(registrationDto));
    }

    @Test
    void createSuperAdmin_alreadyExists() {
        Users existing = Users.builder().roles(Set.of(Role.SUPER_ADMIN)).build();
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.findAll()).thenReturn(List.of(existing));
        assertThrows(IllegalStateException.class, () -> userService.createSuperAdmin(registrationDto));
    }

    /* ------------ superAdminExists ------------ */

    @Test
    void superAdminExists_true() {
        Users u = Users.builder().roles(Set.of(Role.SUPER_ADMIN)).build();
        when(userRepository.findAll()).thenReturn(List.of(u));
        assertTrue(userService.superAdminExists());
    }

    @Test
    void superAdminExists_false() {
        Users u = Users.builder().roles(Set.of(Role.USER)).build();
        when(userRepository.findAll()).thenReturn(List.of(u));
        assertFalse(userService.superAdminExists());
    }

    /* ------------ getUserById ------------ */

    @Test
    void getUserById_success_asAdmin() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(userEntity));
        when(userMapper.toDto(userEntity)).thenReturn(userDto);

        mockAuth(adminPrincipal);

        UserDto dto = userService.getUserById("u1");
        assertEquals("u1", dto.getUserId());
    }

    @Test
    void getUserById_notFound() {
        when(userRepository.findById("x")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.getUserById("x"));
    }

    @Test
    void getUserById_unauthorized() {
        when(userRepository.findById("u2")).thenReturn(Optional.of(userEntity));
        mockAuth(userPrincipal);
        assertThrows(UnauthorizedException.class, () -> userService.getUserById("u2"));
    }

    /* ------------ getUserByEmail ------------ */

    @Test
    void getUserByEmail_success_asSelf() {
        // Arrange
        userEntity = Users.builder()
                .userId("u1")
                .email("john@example.com")
                .build();

        UserPrincipal samePrincipal = UserPrincipal.builder()
                .userId("u1") // same ID to bypass UnauthorizedException
                .email("john@example.com")
                .roles(Set.of(Role.USER))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(userEntity));
        when(userMapper.toDto(userEntity)).thenReturn(userDto);
        mockAuth(samePrincipal); // override only for this test

        // Act & Assert
        assertEquals("john@example.com", userService.getUserByEmail("john@example.com").getEmail());
    }


    @Test
    void getUserByEmail_notFound() {
        when(userRepository.findByEmail("x")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.getUserByEmail("x"));
    }

    /* ------------ getAllUsers ------------ */

    @Test
    void getAllUsers_success_admin() {
        Users u = userEntity;
        when(userRepository.findAll()).thenReturn(List.of(u));
        when(userMapper.toDtoList(List.of(u))).thenReturn(List.of(userDto));

        mockAuth(adminPrincipal);

        List<UserDto> list = userService.getAllUsers();
        assertEquals(1, list.size());
    }

    @Test
    void getAllUsers_notAdmin() {
        mockAuth(userPrincipal);
        assertThrows(UnauthorizedException.class, () -> userService.getAllUsers());
    }

    @Test
    void getAllUsers_invalidPrincipal() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("invalid");
        SecurityContextHolder.setContext(securityContext);
        assertThrows(UnauthorizedException.class, () -> userService.getAllUsers());
    }

    @Test
    void getAllUsers_noAuth() {
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);
        assertThrows(UnauthorizedException.class, () -> userService.getAllUsers());
    }

    /* ------------ updateUser ------------ */

    @Test
    void updateUser_success_emailChange() {
        mockAuth(userPrincipal);
        Users existing = Users.builder()
                .userId("u1")
                .email("old@a.com")
                .roles(Set.of(Role.USER))
                .build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.save(existing)).thenReturn(existing);
        when(userMapper.toDto(existing)).thenReturn(userDto);

        userService.updateUser("u1", userDto);
        verify(userRepository).save(existing);
    }

    @Test
    void updateUser_emailDuplicate() {
        mockAuth(userPrincipal);
        Users existing = Users.builder()
                .userId("u1").email("old@a.com").roles(Set.of(Role.USER)).build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        UserDto changed = UserDto.builder().userId("u1").email("john@example.com").build();
        assertThrows(DuplicateEmailException.class, () -> userService.updateUser("u1", changed));
    }

    @Test
    void updateUser_notFound() {
        mockAuth(userPrincipal);
        when(userRepository.findById("u1")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.updateUser("u1", userDto));
    }

    /* ------------ deleteUser ------------ */

    @Test
    void deleteUser_selfDeletes() {
        mockAuth(userPrincipal);
        when(userRepository.existsById("u1")).thenReturn(true);
        doNothing().when(userRepository).deleteById("u1");

        assertDoesNotThrow(() -> userService.deleteUser("u1"));
        verify(userRepository).deleteById("u1");
    }

    @Test
    void deleteUser_adminDeletesOther() {
        mockAuth(adminPrincipal);
        when(userRepository.existsById("uX")).thenReturn(true);
        doNothing().when(userRepository).deleteById("uX");
        assertDoesNotThrow(() -> userService.deleteUser("uX"));
    }

    @Test
    void deleteUser_unauthorizedUser() {
        mockAuth(userPrincipal);
        assertThrows(UnauthorizedException.class, () -> userService.deleteUser("uX"));
    }

    @Test
    void deleteUser_notFound() {
        mockAuth(adminPrincipal);
        when(userRepository.existsById("missing")).thenReturn(false);
        assertThrows(UserNotFoundException.class, () -> userService.deleteUser("missing"));
    }

    @Test
    void deleteUser_invalidPrincipal() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("string");
        SecurityContextHolder.setContext(securityContext);
        assertThrows(UnauthorizedException.class, () -> userService.deleteUser("u1"));
    }

    /* ------------ assignRoles ------------ */

    @Test
    void assignRoles_success() {
        Users existing = userEntity;
        when(userRepository.findById("u1")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(userMapper.toDto(existing)).thenReturn(userDto);

        UserDto dto = userService.assignRoles("u1", Set.of(Role.USER));
        assertEquals("u1", dto.getUserId());
    }

    @Test
    void assignRoles_notFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.assignRoles("missing", Set.of(Role.USER)));
    }

    /* ------------ validateUserAccess branches ------------ */

    @Test
    void validateUserAccess_notAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.setContext(securityContext);
        assertThrows(UnauthorizedException.class,
                () -> invokeValidate("x"));
    }

    @Test
    void validateUserAccess_superAdminPasses() {
        mockAuth(adminPrincipal);
        assertDoesNotThrow(() -> invokeValidate("x"));
    }

    @Test
    void validateUserAccess_selfPasses() {
        mockAuth(userPrincipal);
        assertDoesNotThrow(() -> invokeValidate("u1"));
    }

    @Test
    void validateUserAccess_unauthorized() {
        mockAuth(userPrincipal);
        assertThrows(UnauthorizedException.class, () -> invokeValidate("other"));
    }

    /* ------------ superAdminExists (Branch 1) ------------ */

    @Test
    void superAdminExists_false_nullRoles() {
        // Covers the 'user.getRoles() != null' check in the stream
        Users userWithNullRoles = Users.builder().roles(null).build();
        when(userRepository.findAll()).thenReturn(List.of(userWithNullRoles));
        assertFalse(userService.superAdminExists());
    }


    /* ------------ updateUser (Branches 2 & 3) ------------ */

    @Test
    void updateUser_success_emailIsNull() {
        // Covers the branch where 'userDto.getEmail() != null' is false
        mockAuth(userPrincipal);
        Users existing = Users.builder().userId("u1").email("old@a.com").build();
        UserDto dtoWithNullEmail = UserDto.builder().email(null).name("New Name").build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        userService.updateUser("u1", dtoWithNullEmail);

        // Verify that 'existsByEmail' was never called because the email was null
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository).save(existing);
    }

    @Test
    void updateUser_success_emailIsSame() {
        // Covers the branch where '!userDto.getEmail().equals(existingUser.getEmail())' is false
        mockAuth(userPrincipal);
        Users existing = Users.builder().userId("u1").email("john@example.com").build();
        UserDto dtoWithSameEmail = UserDto.builder().email("john@example.com").name("New Name").build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        userService.updateUser("u1", dtoWithSameEmail);

        // Verify that 'existsByEmail' was never called because the email was the same
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository).save(existing);
    }


    /* ------------ validateUserAccess (Branches 4 & 5) ------------ */

    @Test
    void validateUserAccess_nullAuthentication() {
        // Covers the 'authentication == null' branch
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        assertThrows(UnauthorizedException.class,
                () -> invokeValidate("x"));
    }

    @Test
    void validateUserAccess_invalidPrincipalType() {
        // Covers the '!(authentication.getPrincipal() instanceof UserPrincipal)' branch
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("a_random_string"); // Not a UserPrincipal
        SecurityContextHolder.setContext(securityContext);

        assertThrows(UnauthorizedException.class,
                () -> invokeValidate("x"));
    }

    @Test
    void deleteUser_noAuth() {
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);
        assertThrows(UnauthorizedException.class, () -> userService.deleteUser("u1"));
    }

    /* ------------ helper ------------ */
    private void mockAuth(UserPrincipal principal) {
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }


    // reflective call to private validateUserAccess to ensure branch coverage
    private void invokeValidate(String id) {
        try {
            var m = UserServiceImpl.class.getDeclaredMethod("validateUserAccess", String.class);
            m.setAccessible(true);
            m.invoke(userService, id);
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException re) throw re;
        }
    }


    private void validateUserAccess(String requestedUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserPrincipal currentUser)) {
            throw new UnauthorizedException("User not authenticated or invalid principal");
        }

        String currentUserId = currentUser.getUserId();

        if (currentUser.getRoles().contains(Role.SUPER_ADMIN)) {
            return;
        }
        if (!currentUserId.equals(requestedUserId)) {
            throw new UnauthorizedException("You don't have permission to access this user's data");
        }
    }
}
