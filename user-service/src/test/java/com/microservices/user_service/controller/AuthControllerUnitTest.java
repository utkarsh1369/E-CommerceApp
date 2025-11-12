package com.microservices.user_service.controller;

import com.microservices.user_service.exception.DuplicateEmailException;
import com.microservices.user_service.model.Role;
import com.microservices.user_service.model.UserPrincipal;
import com.microservices.user_service.model.dto.*;
import com.microservices.user_service.security.JwtUtil;
import com.microservices.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthControllerUnitTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserService userService;
    @Mock
    private AdminSecretProperties adminSecret;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthController authController;

    private UserRegistrationDto registrationDto;
    private UserDto userDto;
    private AuthRequest authRequest;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        registrationDto = new UserRegistrationDto();
        registrationDto.setEmail("john@example.com");
        registrationDto.setPassword("pass123");
        registrationDto.setName("John");

        userDto = new UserDto();
        userDto.setEmail("john@example.com");
        userDto.setName("John");

        authRequest = new AuthRequest("john@example.com", "pass123");

        principal =  UserPrincipal.builder()
                .userId("user1")
                .email("john@example.com")
                .password("pass123")
                .roles(Set.of(Role.USER))
                .build();
    }

    @Test
    void registerUser_ShouldReturnCreated() {
        when(userService.registerUser(registrationDto)).thenReturn(userDto);

        ResponseEntity<UserDto> response = authController.register(registrationDto);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(userDto);
        verify(userService).registerUser(registrationDto);
    }

    @Test
    void registerAdmin_ShouldReturnForbidden_WhenSecretInvalid() {
        when(adminSecret.getCode()).thenReturn("SECRET123");

        ResponseEntity<?> response = authController.registerAdmin(
                registrationDto, "WRONG_SECRET", request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isInstanceOf(ErrorMessage.class);
        verify(userService, never()).createSuperAdmin(any());
    }

    @Test
    void registerAdmin_ShouldReturnGone_WhenSuperAdminAlreadyExists() {
        when(adminSecret.getCode()).thenReturn("SECRET123");
        when(userService.superAdminExists()).thenReturn(true);

        ResponseEntity<?> response = authController.registerAdmin(
                registrationDto, "SECRET123", request);

        assertThat(response.getStatusCode().value()).isEqualTo(410);
        assertThat(response.getBody()).isInstanceOf(ErrorMessage.class);
    }

    @Test
    void registerAdmin_ShouldReturnCreated_WhenSuperAdminCreated() {
        when(adminSecret.getCode()).thenReturn("SECRET123");
        when(userService.superAdminExists()).thenReturn(false);
        when(userService.createSuperAdmin(registrationDto)).thenReturn(userDto);

        ResponseEntity<?> response = authController.registerAdmin(
                registrationDto, "SECRET123", request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isInstanceOf(SuccessMessage.class);
        verify(userService).createSuperAdmin(registrationDto);
    }

    @Test
    void registerAdmin_ShouldReturnConflict_WhenDuplicateEmail() {
        when(adminSecret.getCode()).thenReturn("SECRET123");
        when(userService.superAdminExists()).thenReturn(false);
        when(userService.createSuperAdmin(any()))
                .thenThrow(new DuplicateEmailException("Email already registered"));

        ResponseEntity<?> response = authController.registerAdmin(
                registrationDto, "SECRET123", request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isInstanceOf(ErrorMessage.class);
    }

    @Test
    void login_ShouldReturnOk_WhenAuthenticationSuccessful() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtUtil.generateToken(principal, principal.getUserId()))
                .thenReturn("mock-jwt-token");

        ResponseEntity<AuthResponse> response = authController.login(authRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("mock-jwt-token");
        verify(authenticationManager).authenticate(any());
        verify(jwtUtil).generateToken(principal, principal.getUserId());
    }
}
