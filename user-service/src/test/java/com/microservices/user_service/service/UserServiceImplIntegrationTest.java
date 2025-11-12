package com.microservices.user_service.service;

import com.microservices.user_service.exception.*;
import com.microservices.user_service.model.*;
import com.microservices.user_service.model.dto.*;
import com.microservices.user_service.repository.UserRepository;
import com.microservices.user_service.service.impl.UserServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceImplIntegrationTest {

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private UserRepository userRepository;

    private UserRegistrationDto registrationDto;
    private Users existingUser;

    @BeforeEach
    void setup() {
        registrationDto = UserRegistrationDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .address("123 Test Lane")
                .phoneNumber("1234567890")
                .build();

        existingUser = Users.builder()
                .userId("user123")
                .name("John Doe")
                .email("john@example.com")
                .password("encodedPass123")
                .address("123 Test Lane")
                .phoneNumber("1234567890")
                .roles(Set.of(Role.USER))
                .build();

        userRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    private void setAuth(UserPrincipal principal) {
        var auth = new TestingAuthenticationToken(principal, null);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void registerUser_Success() {
        UserDto saved = userService.registerUser(registrationDto);
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when registering existing email")
    void registerUser_DuplicateEmail() {
        userRepository.save(existingUser);
        assertThrows(DuplicateEmailException.class, () -> userService.registerUser(registrationDto));
    }

    @Test
    @DisplayName("Should create super admin successfully")
    void createSuperAdmin_Success() {
        UserDto admin = userService.createSuperAdmin(registrationDto);
        assertThat(admin.getRoles()).contains(Role.SUPER_ADMIN);
        assertThat(userService.superAdminExists()).isTrue();
    }

    @Test
    @DisplayName("Should get user by email successfully when authorized")
    void getUserByEmail_Authorized() {
        Users user = Users.builder()
                .userId("user123")
                .name("Jane Doe")
                .email("test@example.com")
                .password("encodedPass123")
                .address("123 Street xyz")
                .phoneNumber("1234567890")
                .roles(Set.of(Role.USER))
                .build();
        userRepository.save(user);

        setAuth(UserPrincipal.builder()
                .userId("user123")
                .roles(Set.of(Role.USER))
                .build());
        UserDto result = userService.getUserByEmail("test@example.com");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when accessing another user's data")
    void getUserByEmail_Unauthorized() {
        Users user = new Users();
        user.setUserId("user123");
        user.setName("John Doe");
        user.setEmail("unauth@example.com");
        user.setPassword("encodedPass123");
        user.setAddress("addr");
        user.setPhoneNumber("1234567890");
        user.setRoles(Set.of(Role.USER));
        userRepository.save(user);

        setAuth(UserPrincipal.builder()
                .userId("differentUser")
                .roles(Set.of(Role.USER))
                .build());

        assertThatThrownBy(() -> userService.getUserByEmail("unauth@example.com"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should update user email successfully")
    void updateUser_EmailChanged() {
        Users user = Users.builder()
                .userId("user123")
                .name("Jane Doe")
                .email("test@example.com")
                .password("encodedPass123")
                .address("123 Street xyz")
                .phoneNumber("1234567890")
                .roles(Set.of(Role.USER))
                .build();
        userRepository.save(user);

        setAuth(UserPrincipal.builder()
                .userId("user123")
                .roles(Set.of(Role.USER))
                .build());

        UserDto dto = new UserDto();
        dto.setEmail("new@example.com");

        UserDto result = userService.updateUser("user123", dto);

        assertThat(result.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when super admin already exists")
    void createSuperAdmin_AlreadyExists() {
        userRepository.save(Users.builder()
                .userId("user123")
                .email("test@example.com")
                .name("John Doe")
                .address("123 Street xy")
                .password("ValidPass123")
                .phoneNumber("1234567890")
                .roles(Set.of(Role.SUPER_ADMIN))
                .build());
        assertThrows(IllegalStateException.class, () -> userService.createSuperAdmin(registrationDto));
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when email already registered for super admin")
    void createSuperAdmin_DuplicateEmail() {
        userRepository.save(existingUser);
        assertThrows(DuplicateEmailException.class, () -> userService.createSuperAdmin(registrationDto));
    }

    @Test
    @DisplayName("Should fetch user by ID successfully (authorized self-access)")
    void getUserById_Success() {
        userRepository.save(existingUser);
        setAuth(UserPrincipal.builder()
                .userId("user123")
                .email("john@example.com")
                .roles(Set.of(Role.USER))
                .build());

        UserDto result = userService.getUserById("user123");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when accessing another user’s data")
    void getUserById_Unauthorized() {
        userRepository.save(existingUser);
        setAuth(UserPrincipal.builder()
                .userId("otherUser")
                .email("other@test.com")
                .roles(Set.of(Role.USER))
                .build());

        assertThrows(UnauthorizedException.class, () -> userService.getUserById("user123"));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user not authenticated")
    void getUserById_Unauthenticated() {
        userRepository.save(existingUser);
        SecurityContextHolder.clearContext();
        assertThrows(UnauthorizedException.class, () -> userService.getUserById("user123"));
    }

    @Test
    @DisplayName("Should allow SUPER_ADMIN to access any user data")
    void getUserById_SuperAdminAccess() {
        userRepository.save(existingUser);
        setAuth(UserPrincipal.builder()
                .userId("admin123")
                .email("admin@example.com")
                .roles(Set.of(Role.SUPER_ADMIN))
                .build());

        UserDto result = userService.getUserById("user123");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when fetching non-existent user by ID")
    void getUserById_NotFound() {
        setAuth(UserPrincipal.builder()
                .userId("u1")
                .roles(Set.of(Role.SUPER_ADMIN))
                .build());
        assertThrows(UserNotFoundException.class, () -> userService.getUserById("missing"));
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when email not found")
    void getUserByEmail_NotFound() {
        setAuth(UserPrincipal.builder()
                .userId("u1")
                .roles(Set.of(Role.SUPER_ADMIN))
                .build());
        assertThrows(UserNotFoundException.class, () -> userService.getUserByEmail("notfound@example.com"));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-admin tries to access another user's email")
    void getUserByEmail_NotAdminUnauthorized() {
        userRepository.save(existingUser);

        setAuth(UserPrincipal.builder()
                .userId("otherUser")
                .roles(Set.of(Role.USER))
                .build());

        assertThrows(UnauthorizedException.class, () -> userService.getUserByEmail("john@example.com"));
    }

    @Test
    @DisplayName("Should update user fields successfully (excluding email)")
    void updateUser_Success() {
        userRepository.save(existingUser);
        setAuth(UserPrincipal.builder()
                .userId("user123")
                .email("john@example.com")
                .roles(Set.of(Role.USER))
                .build());

        UserDto update = UserDto.builder()
                .name("Updated Name")
                .address("Updated Address")
                .build();

        UserDto result = userService.updateUser("user123", update);
        assertThat(result.getName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when updating to existing email")
    void updateUser_DuplicateEmail() {
        existingUser.setName("John Doe");
        existingUser.setPassword("strongPass123");
        existingUser.setAddress("Main Street 1");
        existingUser.setPhoneNumber("9876543210");
        existingUser.setRoles(Set.of(Role.USER));
        userRepository.save(existingUser);

        userRepository.save(Users.builder()
                .userId("otherUser")
                .name("Jane Doe")
                .email("taken@example.com")
                .password("encodedPass999")
                .address("123 Street xyz")
                .phoneNumber("1234567890")
                .roles(Set.of(Role.USER))
                .build());
        setAuth(UserPrincipal.builder()
                .userId("user123")
                .email("john@example.com")
                .roles(Set.of(Role.USER))
                .build());

        UserDto update = UserDto.builder()
                .email("taken@example.com")
                .build();

        assertThrows(DuplicateEmailException.class,
                () -> userService.updateUser("user123", update));
    }

    @Test
    @DisplayName("Should update user successfully when email not changed")
    void updateUser_EmailUnchanged() {
        userRepository.save(Users.builder()
                .userId("user1")
                .name("Jane Doe")
                .password("encodedPass999")
                .address("123 Street xyz")
                .phoneNumber("1234567890")
                .roles(Set.of(Role.USER))
                .build());

        setAuth(UserPrincipal.builder().userId("user1").roles(Set.of(Role.USER)).build());

        UserDto dto = new UserDto();
        dto.setName("Johnny");

        UserDto updated = userService.updateUser("user1", dto);

        assertThat(updated.getName()).isEqualTo("Johnny");
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when updating non-existent user")
    void updateUser_NotFound() {
        setAuth(UserPrincipal.builder()
                .userId("admin123")
                .roles(Set.of(Role.SUPER_ADMIN)) // admin has permission
                .build());

        assertThrows(UserNotFoundException.class, () ->
                userService.updateUser("unknown", UserDto.builder().name("X").build()));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user tries to update another user's data")
    void updateUser_UnauthorizedAccess() {
        userRepository.save(existingUser);

        setAuth(UserPrincipal.builder()
                .userId("otherUser")
                .roles(Set.of(Role.USER))
                .build());

        UserDto update = UserDto.builder().name("Intruder Edit").build();

        assertThrows(UnauthorizedException.class,
                () -> userService.updateUser("user123", update));
    }

    @Test
    @DisplayName("Should delete existing user successfully")
    void deleteUser_Success() {
        existingUser.setUserId("admin123"); // make sure it matches the one you're deleting
        userRepository.save(existingUser);

        setAuth(UserPrincipal.builder()
                .userId("admin123")
                .roles(Set.of(Role.SUPER_ADMIN)) // has permission
                .build());

        userService.deleteUser("admin123");

        assertThat(userRepository.existsById("admin123")).isFalse();
    }


    @Test
    @DisplayName("Should throw UserNotFoundException when deleting non-existent user")
    void deleteUser_NotFound() {
        setAuth(UserPrincipal.builder()
                .userId("admin123")
                .roles(Set.of(Role.SUPER_ADMIN))
                .build());
        assertThrows(UserNotFoundException.class, () -> userService.deleteUser("missing"));
    }


    @Test
    @DisplayName("Should throw UnauthorizedException when deleting user without admin rights")
    void deleteUser_UnauthorizedAccess() {
        userRepository.save(existingUser);

        setAuth(UserPrincipal.builder()
                .userId("randomUser")
                .roles(Set.of(Role.USER))
                .build());

        assertThrows(UnauthorizedException.class, () -> userService.deleteUser("user123"));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when no authentication is present")
    void deleteUser_NoAuthentication() {
        // Clear any existing authentication
        SecurityContextHolder.clearContext();

        // When there's no authenticated user, this should throw UnauthorizedException
        assertThrows(UnauthorizedException.class, () -> userService.deleteUser("someUserId"));
    }

    @Test
    @DisplayName("Should assign roles successfully")
    void assignRoles_Success() {
        existingUser.setRoles(new HashSet<>(Set.of(Role.USER)));
        userRepository.save(existingUser);
        Set<Role> newRoles =new HashSet<>( Set.of(Role.USER, Role.SUPER_ADMIN));
        UserDto result = userService.assignRoles("user123", newRoles);
        assertThat(result.getRoles()).containsExactlyInAnyOrder(Role.USER, Role.SUPER_ADMIN);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when assigning roles to missing user")
    void assignRoles_NotFound() {
        assertThrows(UserNotFoundException.class, () ->
                userService.assignRoles("missing", Set.of(Role.USER)));
    }

    @Test
    @DisplayName("Should fetch all users when SUPER_ADMIN is authenticated")
    void getAllUsers_Success() {
        userRepository.save(existingUser);
        userRepository.save(Users.builder()
                .userId("u2")
                .email("u2@example.com")
                .name("User Two")
                .password("password987")
                .address("Test Avenue")
                .roles(Set.of(Role.USER))
                .build());
        setAuth(UserPrincipal.builder()
                .userId("super_admin")
                .email("admin@example.com")
                .roles(Set.of(Role.SUPER_ADMIN))
                .build());

        List<UserDto> all = userService.getAllUsers();

        assertThat(all).hasSize(2);
    }


    @Test
    @DisplayName("Should return empty list when no users exist and SUPER_ADMIN is authenticated")
    void getAllUsers_EmptyList() {
        setAuth(UserPrincipal.builder()
                .userId("super_admin")
                .email("admin@example.com")
                .roles(Set.of(Role.SUPER_ADMIN))
                .build());

        List<UserDto> result = userService.getAllUsers();

        assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("Should throw UnauthorizedException when non-super-admin tries to fetch all users")
    void getAllUsers_Unauthorized() {
        userRepository.save(existingUser);

        setAuth(UserPrincipal.builder()
                .userId("user123")
                .roles(Set.of(Role.USER))
                .build());

        assertThrows(UnauthorizedException.class, () -> userService.getAllUsers());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when no authentication present")
    void getAllUsers_NoAuthentication() {
        // Clear any existing authentication
        SecurityContextHolder.clearContext();

        assertThrows(UnauthorizedException.class, () -> userService.getAllUsers());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when authentication principal is invalid type")
    void getAllUsers_InvalidPrincipal() {
        var auth = new TestingAuthenticationToken("invalidPrincipal", null);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(UnauthorizedException.class, () -> userService.getAllUsers());
    }

}
