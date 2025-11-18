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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationDto registrationDto;
    private Users userEntity;
    private UserDto userDto;

    @BeforeEach
    void setup() {
        SecurityContextHolder.setContext(securityContext);

        registrationDto = UserRegistrationDto.builder()
                .name("John")
                .email("john@test.com")
                .password("pass")
                .build();

        userEntity = Users.builder()
                .userId("u1")
                .email("john@test.com")
                .roles(new HashSet<>(Set.of(Role.USER)))
                .build();

        userDto = UserDto.builder()
                .userId("u1")
                .email("john@test.com")
                .roles(Set.of(Role.USER))
                .build();
    }

    private void mockSecurityContext(String userId, String email, Set<Role> roles) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(userId)
                .email(email)
                .roles(roles)
                .build();

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
    }

    @Test
    void registerUser_Success() {
        when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPass");
        when(userMapper.toEntity(any(), any())).thenReturn(userEntity);
        when(userRepository.save(any())).thenReturn(userEntity);
        when(userMapper.toDto(any())).thenReturn(userDto);

        assertNotNull(userService.registerUser(registrationDto));
    }

    @Test
    void registerUser_DuplicateEmail() {
        when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(true);
        assertThrows(DuplicateEmailException.class, () -> userService.registerUser(registrationDto));
    }

    @Test
    void createSuperAdmin_Success() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(userEntity);
        when(userMapper.toDto(any())).thenReturn(userDto);

        assertNotNull(userService.createSuperAdmin(registrationDto));
    }

    @Test
    void createSuperAdmin_DuplicateEmail() {
        when(userRepository.existsByEmail(any())).thenReturn(true);
        assertThrows(DuplicateEmailException.class, () -> userService.createSuperAdmin(registrationDto));
    }

    @Test
    void createSuperAdmin_AlreadyExists() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        Users existingAdmin = Users.builder().roles(Set.of(Role.SUPER_ADMIN)).build();
        when(userRepository.findAll()).thenReturn(List.of(existingAdmin));

        assertThrows(IllegalStateException.class, () -> userService.createSuperAdmin(registrationDto));
    }

    @Test
    void getUserById_Success_AsSelf() {
        mockSecurityContext("u1", "john@test.com", Set.of(Role.USER));
        when(userRepository.findById("u1")).thenReturn(Optional.of(userEntity));
        when(userMapper.toDto(userEntity)).thenReturn(userDto);

        UserDto result = userService.getUserById("u1");
        assertEquals("u1", result.getUserId());
    }

    @Test
    void getUserById_Success_AsAdmin() {
        mockSecurityContext("admin", "admin@test.com", Set.of(Role.SUPER_ADMIN));
        when(userRepository.findById("u1")).thenReturn(Optional.of(userEntity));
        when(userMapper.toDto(userEntity)).thenReturn(userDto);

        userService.getUserById("u1");
        verify(userMapper).toDto(userEntity);
    }

    @Test
    void getUserById_Unauthorized_AsOther() {
        mockSecurityContext("u2", "other@test.com", Set.of(Role.USER));
        when(userRepository.findById("u1")).thenReturn(Optional.of(userEntity));

        assertThrows(UnauthorizedException.class, () -> userService.getUserById("u1"));
    }

    @Test
    void getUserById_NotFound() {
        when(userRepository.findById("u1")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.getUserById("u1"));
    }

    @Test
    void getUserByEmail_Success_AsSelf() {
        mockSecurityContext("u1", "john@test.com", Set.of(Role.USER));

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(userEntity));
        when(userMapper.toDto(userEntity)).thenReturn(userDto);

        userService.getUserByEmail("john@test.com");
    }

    @Test
    void getUserByEmail_Success_AsAdmin_Bypass() {
        mockSecurityContext("admin", "admin@test.com", Set.of(Role.SUPER_ADMIN));

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(userEntity));
        when(userMapper.toDto(userEntity)).thenReturn(userDto);

        userService.getUserByEmail("john@test.com");
    }

    @Test
    void getUserByEmail_Unauthorized() {
        mockSecurityContext("u2", "u2@test.com", Set.of(Role.USER));

        assertThrows(UnauthorizedException.class, () -> userService.getUserByEmail("john@test.com"));
    }

    @Test
    void getUserByEmail_NotFound() {
        mockSecurityContext("admin", "admin@test.com", Set.of(Role.SUPER_ADMIN));
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserByEmail("john@test.com"));
    }

    @Test
    void getUserByEmail_NotAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(null);
        assertThrows(UnauthorizedException.class, () -> userService.getUserByEmail("x"));
    }

    @Test
    void updateUser_Success_EmailNull_SkipsCheck() {
        mockSecurityContext("u1", "john@test.com", Set.of(Role.USER));
        when(userRepository.findById("u1")).thenReturn(Optional.of(userEntity));
        when(userRepository.save(any())).thenReturn(userEntity);
        when(userMapper.toDto(any())).thenReturn(userDto);

        UserDto updateDto = UserDto.builder().email(null).name("New Name").build();

        userService.updateUser("u1", updateDto);

        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void updateUser_Success_EmailSame_SkipsCheck() {
        mockSecurityContext("u1", "john@test.com", Set.of(Role.USER));

        userEntity.setEmail("john@test.com");
        when(userRepository.findById("u1")).thenReturn(Optional.of(userEntity));
        when(userRepository.save(any())).thenReturn(userEntity);
        when(userMapper.toDto(any())).thenReturn(userDto);

        UserDto updateDto = UserDto.builder().email("john@test.com").name("New Name").build();

        userService.updateUser("u1", updateDto);

        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void updateUser_Success_EmailChanged_Valid() {
        mockSecurityContext("u1", "john@test.com", Set.of(Role.USER));
        when(userRepository.findById("u1")).thenReturn(Optional.of(userEntity));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(userEntity);
        when(userMapper.toDto(any())).thenReturn(userDto);

        UserDto updateDto = UserDto.builder().email("new@test.com").build();
        userService.updateUser("u1", updateDto);

        verify(userRepository).existsByEmail("new@test.com");
    }

    @Test
    void updateUser_Success_EmailSameIgnoreCase_SkipsCheck() {
        mockSecurityContext("u1", "john@test.com", Set.of(Role.USER));

        userEntity.setEmail("john@test.com");
        when(userRepository.findById("u1")).thenReturn(Optional.of(userEntity));
        when(userRepository.save(any())).thenReturn(userEntity);
        when(userMapper.toDto(any())).thenReturn(userDto);

        UserDto updateDto = UserDto.builder().email("JOHN@test.com").name("New Name").build();

        userService.updateUser("u1", updateDto);

        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void updateUser_DuplicateEmail() {
        mockSecurityContext("u1", "john@test.com", Set.of(Role.USER));
        when(userRepository.findById("u1")).thenReturn(Optional.of(userEntity));
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        UserDto updateDto = UserDto.builder().email("taken@test.com").build();

        assertThrows(DuplicateEmailException.class, () -> userService.updateUser("u1", updateDto));
    }

    @Test
    void deleteUser_Success_AsSelf() {
        mockSecurityContext("u1", "john@test.com", Set.of(Role.USER));
        when(userRepository.existsById("u1")).thenReturn(true);

        userService.deleteUser("u1");
        verify(userRepository).deleteById("u1");
    }

    @Test
    void deleteUser_Success_AsAdmin_DeletingOther() {
        mockSecurityContext("admin", "admin@test.com", Set.of(Role.SUPER_ADMIN));
        when(userRepository.existsById("u1")).thenReturn(true);

        userService.deleteUser("u1");
        verify(userRepository).deleteById("u1");
    }

    @Test
    void deleteUser_Unauthorized() {
        mockSecurityContext("u2", "u2@test.com", Set.of(Role.USER));
        assertThrows(UnauthorizedException.class, () -> userService.deleteUser("u1"));
    }

    @Test
    void deleteUser_NotFound() {
        mockSecurityContext("admin", "admin@test.com", Set.of(Role.SUPER_ADMIN));
        when(userRepository.existsById("u1")).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser("u1"));
    }

    void deleteUser_NullAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> userService.deleteUser("u1"));
    }

    @Test
    void deleteUser_PrincipalIsNull() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> userService.deleteUser("u1"));
    }

    @Test
    void deleteUser_WrongPrincipalType() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("WRONG_PRINCIPAL_TYPE");

        assertThrows(UnauthorizedException.class, () -> userService.deleteUser("u1"));
    }

    @Test
    void getAllUsers_Success() {
        mockSecurityContext("admin", "admin@test.com", Set.of(Role.SUPER_ADMIN));
        Pageable pageable = PageRequest.of(0, 10);
        Page<Users> mockUserPage = new PageImpl<>(List.of(userEntity), pageable, 1);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(mockUserPage);
        when(userMapper.toDto(any(Users.class))).thenReturn(userDto);
        Page<UserDto> resultPage = userService.getAllUsers(pageable);
        assertEquals(1, resultPage.getTotalElements());
        assertEquals(1, resultPage.getContent().size());
        assertEquals(userDto.getName(), resultPage.getContent().getFirst().getName());
        verify(userRepository, times(1)).findAll(any(Pageable.class));
        verify(userMapper, times(1)).toDto(any(Users.class));
    }

    @Test
    void getAllUsers_Unauthorized_Role() {
        mockSecurityContext("u1", "john@test.com", Set.of(Role.USER));
        Pageable pageable = PageRequest.of(0, 10);
        assertThrows(UnauthorizedException.class, () -> userService.getAllUsers(pageable));
    }

    @Test
    void getAllUsers_NoAuth() {
        when(securityContext.getAuthentication()).thenReturn(null);
        Pageable pageable = PageRequest.of(0, 10);
        assertThrows(UnauthorizedException.class, () -> userService.getAllUsers(pageable));
    }

    @Test
    void getAllUsers_WrongPrincipalType() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("STRING_PRINCIPAL");
        Pageable pageable = PageRequest.of(0, 10);
        assertThrows(UnauthorizedException.class, () -> userService.getAllUsers(pageable));
    }

    @Test
    void assignRoles_Success() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(userEntity));
        when(userRepository.save(any())).thenReturn(userEntity);
        when(userMapper.toDto(any())).thenReturn(userDto);

        userService.assignRoles("u1", Set.of(Role.SUPER_ADMIN));
        verify(userRepository).save(userEntity);
    }

    @Test
    void assignRoles_NotFound() {
        when(userRepository.findById("u1")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.assignRoles("u1", Set.of(Role.USER)));
    }

    @Test
    void validateUserAccess_NullAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);

        UserDto dummyDto = new UserDto();
        assertThrows(UnauthorizedException.class, () -> userService.updateUser("u1", dummyDto));
    }

    @Test
    void validateUserAccess_NotAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        UserDto dummyDto = new UserDto();
        assertThrows(UnauthorizedException.class, () -> userService.updateUser("u1", dummyDto));
    }

    @Test
    void validateUserAccess_WrongPrincipal() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("WRONG_PRINCIPAL_TYPE");

        UserDto dummyDto = new UserDto();
        assertThrows(UnauthorizedException.class, () -> userService.updateUser("u1", dummyDto));
    }

    @Test
    void superAdminExists_NullRoles_False() {
        Users u = new Users();
        u.setRoles(null);
        when(userRepository.findAll()).thenReturn(List.of(u));
        assertFalse(userService.superAdminExists());
    }

    @Test
    void superAdminExists_UserWithStandardRole_ReturnsFalse() {
        Users u = Users.builder().roles(Set.of(Role.USER)).build();
        when(userRepository.findAll()).thenReturn(List.of(u));
        assertFalse(userService.superAdminExists());
    }
}