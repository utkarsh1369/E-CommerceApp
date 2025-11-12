package com.microservices.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.user_service.exception.DuplicateEmailException;
import com.microservices.user_service.model.Role;
import com.microservices.user_service.model.UserPrincipal;
import com.microservices.user_service.model.dto.*;
import com.microservices.user_service.security.JwtUtil;
import com.microservices.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AdminSecretProperties adminSecret;

    private UserRegistrationDto registrationDto;
    private UserDto userDto;
    private UserDto adminDto;
    private AuthRequest authRequest;
    private UserPrincipal userPrincipal;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setup() {
        registrationDto = UserRegistrationDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("StrongPass123")
                .phoneNumber("9999999999")
                .address("123 Street")
                .build();

        userDto = UserDto.builder()
                .userId("u1")
                .name("John Doe")
                .email("john@example.com")
                .phoneNumber("9999999999")
                .address("123 Street")
                .roles(Set.of(Role.USER))
                .build();

        adminDto = UserDto.builder()
                .userId("admin1")
                .name("Admin User")
                .email("admin@example.com")
                .phoneNumber("8888888888")
                .address("Admin Street")
                .roles(Set.of(Role.SUPER_ADMIN))
                .build();

        authRequest = AuthRequest.builder()
                .email("john@example.com")
                .password("StrongPass123")
                .build();

        userPrincipal = UserPrincipal.builder()
                .userId("u1")
                .email("john@example.com")
                .roles(Set.of(Role.USER))
                .build();

        adminPrincipal = UserPrincipal.builder()
                .userId("admin1")
                .email("admin@example.com")
                .roles(Set.of(Role.SUPER_ADMIN))
                .build();
    }

    // ==================== REGISTER USER TESTS ====================

    @Test
    @DisplayName("POST /register-user should register a new user successfully")
    void registerUser_success() throws Exception {
        when(userService.registerUser(any(UserRegistrationDto.class))).thenReturn(userDto);

        mockMvc.perform(post("/api/v1/auth/register-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("u1"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.phoneNumber").value("9999999999"))
                .andExpect(jsonPath("$.address").value("123 Street"));

        verify(userService, times(1)).registerUser(any(UserRegistrationDto.class));
    }

    @Test
    @DisplayName("POST /register-user should return 400 for invalid input")
    void registerUser_invalidInput() throws Exception {
        UserRegistrationDto invalidDto = UserRegistrationDto.builder()
                .name("")
                .email("invalid-email")
                .password("weak")
                .build();

        mockMvc.perform(post("/api/v1/auth/register-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any(UserRegistrationDto.class));
    }

    @Test
    @DisplayName("POST /register-user should handle duplicate email exception")
    void registerUser_duplicateEmail() throws Exception {
        when(userService.registerUser(any(UserRegistrationDto.class)))
                .thenThrow(new DuplicateEmailException("Email already exists"));

        mockMvc.perform(post("/api/v1/auth/register-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isConflict());
    }

    // ==================== REGISTER ADMIN TESTS ====================

    @Test
    @DisplayName("POST /register-admin should return 403 if secret code is invalid")
    void registerAdmin_invalidSecret() throws Exception {
        when(adminSecret.getCode()).thenReturn("correct-code");

        mockMvc.perform(post("/api/v1/auth/register-admin")
                        .header("X-Admin-Secret", "wrong-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Invalid admin secret code"));

        verify(userService, never()).superAdminExists();
        verify(userService, never()).createSuperAdmin(any());
    }

    @Test
    @DisplayName("POST /register-admin should return 400 if secret header is missing")
    void registerAdmin_missingSecretHeader() throws Exception {
        when(adminSecret.getCode()).thenReturn("correct-code");

        mockMvc.perform(post("/api/v1/auth/register-admin")
                        .header("X-Admin-Secret", "") // Sending an empty header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isForbidden());

        verify(userService, never()).superAdminExists();
    }

    @Test
    @DisplayName("POST /register-admin should create super admin if valid secret and none exists")
    void registerAdmin_success() throws Exception {
        when(adminSecret.getCode()).thenReturn("secret123");
        when(userService.superAdminExists()).thenReturn(false);
        when(userService.createSuperAdmin(any(UserRegistrationDto.class))).thenReturn(adminDto);

        mockMvc.perform(post("/api/v1/auth/register-admin")
                        .header("X-Admin-Secret", "secret123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(
                        "Super Admin created successfully with email: admin@example.com. This endpoint is now disabled for security."));

        verify(userService, times(1)).superAdminExists();
        verify(userService, times(1)).createSuperAdmin(any(UserRegistrationDto.class));
    }

    @Test
    @DisplayName("POST /register-admin should return GONE if super admin already exists")
    void registerAdmin_alreadyExists() throws Exception {
        when(adminSecret.getCode()).thenReturn("secret123");
        when(userService.superAdminExists()).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/register-admin")
                        .header("X-Admin-Secret", "secret123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value("Super Admin already exists. This endpoint is now permanently disabled for security."));

        verify(userService, times(1)).superAdminExists();
        verify(userService, never()).createSuperAdmin(any());
    }

    @Test
    @DisplayName("POST /register-admin should handle DuplicateEmailException")
    void registerAdmin_duplicateEmail() throws Exception {
        when(adminSecret.getCode()).thenReturn("secret123");
        when(userService.superAdminExists()).thenReturn(false);
        when(userService.createSuperAdmin(any(UserRegistrationDto.class)))
                .thenThrow(new DuplicateEmailException("Email already registered"));

        mockMvc.perform(post("/api/v1/auth/register-admin")
                        .header("X-Admin-Secret", "secret123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered: john@example.com"));

        verify(userService, times(1)).superAdminExists();
        verify(userService, times(1)).createSuperAdmin(any(UserRegistrationDto.class));
    }

    @Test
    @DisplayName("POST /register-admin should handle generic exceptions during admin creation")
    void registerAdmin_genericException() throws Exception {
        when(adminSecret.getCode()).thenReturn("secret123");
        when(userService.superAdminExists()).thenReturn(false);
        when(userService.createSuperAdmin(any(UserRegistrationDto.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/api/v1/auth/register-admin")
                        .header("X-Admin-Secret", "secret123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isInternalServerError());
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("POST /login should authenticate user and return token")
    void login_userSuccess() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(userPrincipal, "u1")).thenReturn("jwt-token-user");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-user"))
                .andExpect(jsonPath("$.userId").value("u1"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.roles").isArray());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, times(1)).generateToken(userPrincipal, "u1");
    }

    @Test
    @DisplayName("POST /login should authenticate admin and return token with admin roles")
    void login_adminSuccess() throws Exception {
        AuthRequest adminAuthRequest = AuthRequest.builder()
                .email("admin@example.com")
                .password("AdminPass123")
                .build();

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(adminPrincipal);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(adminPrincipal, "admin1")).thenReturn("jwt-token-admin");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminAuthRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-admin"))
                .andExpect(jsonPath("$.userId").value("admin1"))
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    @DisplayName("POST /login should return 401 for invalid credentials")
    void login_invalidCredentials() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized());

        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("POST /login should throw exception when authentication fails with generic exception")
    void login_authenticationException() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Authentication service unavailable"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isInternalServerError());

        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("POST /login should return 400 for invalid request body")
    void login_invalidRequestBody() throws Exception {
        AuthRequest invalidRequest = AuthRequest.builder()
                .email("")
                .password("")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("POST /login should handle null principal gracefully")
    void login_nullPrincipal() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(null);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /login should handle token generation failure")
    void login_tokenGenerationFailure() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(userPrincipal, "u1"))
                .thenThrow(new RuntimeException("Token generation failed"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /login should properly map authentication request to token")
    void login_verifyAuthenticationMapping() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(eq(userPrincipal), eq("u1"))).thenReturn("mapped-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mapped-token"));

        // Verify correct parameters were passed
        verify(authenticationManager).authenticate(
                org.mockito.ArgumentMatchers.argThat(token ->
                        token.getPrincipal().equals("john@example.com") &&
                                token.getCredentials().equals("StrongPass123")
                )
        );
    }
}