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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationDto registrationDto;
    private Users userEntity;
    private UserDto userDto;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        registrationDto = UserRegistrationDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .address("Test Street")
                .phoneNumber("1234567890")
                .build();

        userEntity = Users.builder()
                .userId("user123")
                .name("John Doe")
                .email("john@example.com")
                .password("encodedPass")
                .roles(Set.of(Role.USER))
                .build();

        userDto = UserDto.builder()
                .userId("user123")
                .email("john@example.com")
                .name("John Doe")
                .roles(Set.of(Role.USER))
                .build();

        principal = UserPrincipal.builder()
                .userId("user123")
                .email("john@example.com")
                .password("encodedPass")
                .roles(Set.of(Role.USER))
                .build();
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void registerUser_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(userMapper.toEntity(any(UserRegistrationDto.class), anyString())).thenReturn(userEntity);
        when(userRepository.save(any(Users.class))).thenReturn(userEntity);
        when(userMapper.toDto(any(Users.class))).thenReturn(userDto);
        UserDto result = userService.registerUser(registrationDto);
        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository).save(any(Users.class));
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when registering existing email")
    void registerUser_DuplicateEmail() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        assertThrows(DuplicateEmailException.class,
                () -> userService.registerUser(registrationDto));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create super admin successfully")
    void createSuperAdmin_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(userRepository.save(any(Users.class))).thenReturn(userEntity);
        when(userMapper.toDto(any(Users.class))).thenReturn(userDto);
        UserDto result = userService.createSuperAdmin(registrationDto);
        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository).save(any());
    }

    @Test
    @DisplayName("Should throw when super admin already exists")
    void createSuperAdmin_WhenAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        Users existingAdmin = Users.builder()
                .userId("admin1")
                .roles(Set.of(Role.SUPER_ADMIN))
                .build();
        when(userRepository.findAll()).thenReturn(List.of(existingAdmin));

        assertThrows(IllegalStateException.class,
                () -> userService.createSuperAdmin(registrationDto));
    }

    @Test
    @DisplayName("Should return user when found and authorized")
    void getUserById_Success() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
        when(userRepository.findById(anyString())).thenReturn(Optional.of(userEntity));
        when(userMapper.toDto(any(Users.class))).thenReturn(userDto);
        UserDto result = userService.getUserById("user123");
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository).findById("user123");
    }

    @Test
    @DisplayName("Should throw when unauthorized access to another user")
    void getUserById_Unauthorized() {
        UserPrincipal otherUser = UserPrincipal.builder()
                .userId("other123")
                .roles(Set.of(Role.USER))
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(otherUser);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
        when(userRepository.findById("user123")).thenReturn(Optional.of(userEntity));
        assertThrows(UnauthorizedException.class,
                () -> userService.getUserById("user123"));
    }

    @Test
    @DisplayName("Should throw when user not found")
    void getUserById_NotFound() {
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.getUserById("missing"));
    }

    @Test
    @DisplayName("Should update user email successfully")
    void updateUser_Success() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
        Users existing = Users.builder()
                .userId("user123")
                .email("old@example.com")
                .build();
        UserDto updateDto = UserDto.builder()
                .email("new@example.com")
                .build();
        when(userRepository.findById("user123")).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(existing);
        when(userMapper.toDto(any())).thenReturn(userDto);
        UserDto result = userService.updateUser("user123", updateDto);
        assertNotNull(result);
        verify(userRepository).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when updating with existing email")
    void updateUser_DuplicateEmail() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
        Users existing = Users.builder()
                .userId("user123")
                .email("old@example.com")
                .build();
        UserDto updateDto = UserDto.builder().email("taken@example.com").build();
        when(userRepository.findById("user123")).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
        assertThrows(DuplicateEmailException.class,
                () -> userService.updateUser("user123", updateDto));
    }

    @Test
    @DisplayName("Should delete user successfully")
    void deleteUser_Success() {
        when(userRepository.existsById("user123")).thenReturn(true);
        userService.deleteUser("user123");
        verify(userRepository).deleteById("user123");
    }

    @Test
    @DisplayName("Should throw when deleting non-existent user")
    void deleteUser_NotFound() {
        when(userRepository.existsById("missing")).thenReturn(false);
        assertThrows(UserNotFoundException.class,
                () -> userService.deleteUser("missing"));
    }

    @Test
    @DisplayName("Should assign roles successfully")
    void assignRoles_Success() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(userEntity));
        when(userRepository.save(any())).thenReturn(userEntity);
        when(userMapper.toDto(any())).thenReturn(userDto);
        UserDto result = userService.assignRoles("user123", Set.of(Role.SUPER_ADMIN));
        assertNotNull(result);
        verify(userRepository).save(any());
    }
}
