package com.microservices.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.user_service.exception.DuplicateEmailException;
import com.microservices.user_service.model.Role;
import com.microservices.user_service.model.UserPrincipal;
import com.microservices.user_service.model.dto.*;
import com.microservices.user_service.security.JwtUtil;
import com.microservices.user_service.security.SecurityConfig;
import com.microservices.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class AuthControllerTest {

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
    private UserDto registeredUser;
    private AuthRequest authRequest;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        registrationDto = UserRegistrationDto.builder()
                .name("John Doe")
                .email("test@example.com")
                .password("password123")
                .phoneNumber("9876543210")
                .address("Some Street")
                .build();

        registeredUser = UserDto.builder()
                .userId("user123")
                .name("John Doe")
                .email("test@example.com")
                .roles(Set.of(Role.USER))
                .build();

        authRequest = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        userPrincipal =  UserPrincipal.builder()
                .userId("user123")
                .email("test@example.com")
                .password("password123")
                .roles(Set.of(Role.USER))
                .build();
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void registerUser_ShouldReturnCreated() throws Exception {
        when(userService.registerUser(any(UserRegistrationDto.class))).thenReturn(registeredUser);

        mockMvc.perform(post("/api/v1/auth/register-user")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.userId").value("user123"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));

        verify(userService, times(1)).registerUser(any(UserRegistrationDto.class));
    }

    @Test
    @DisplayName("Should register super admin successfully with valid secret and no existing admin")
    void registerAdmin_ValidSecret_ShouldCreateAdmin() throws Exception {
        when(adminSecret.getCode()).thenReturn("secret123");
        when(userService.superAdminExists()).thenReturn(false);
        when(userService.createSuperAdmin(any(UserRegistrationDto.class))).thenReturn(registeredUser);

        mockMvc.perform(post("/api/v1/auth/register-admin")
                        .with(csrf())
                        .header("X-Admin-Secret", "secret123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());

        verify(userService, times(1)).createSuperAdmin(any(UserRegistrationDto.class));
    }

    @Test
    @DisplayName("Should return 403 when admin secret is invalid")
    void registerAdmin_InvalidSecret_ShouldReturnForbidden() throws Exception {
        when(adminSecret.getCode()).thenReturn("realSecret");

        mockMvc.perform(post("/api/v1/auth/register-admin")
                        .with(csrf())
                        .header("X-Admin-Secret", "wrongSecret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Invalid admin secret code"));

        verify(userService, never()).createSuperAdmin(any(UserRegistrationDto.class));
    }

    @Test
    @DisplayName("Should return 410 when super admin already exists")
    void registerAdmin_WhenSuperAdminExists_ShouldReturnGone() throws Exception {
        when(adminSecret.getCode()).thenReturn("secret123");
        when(userService.superAdminExists()).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/register-admin")
                        .with(csrf())
                        .header("X-Admin-Secret", "secret123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value("Super Admin already exists. This endpoint is now permanently disabled for security."));

        verify(userService, never()).createSuperAdmin(any(UserRegistrationDto.class));
    }

    @Test
    @DisplayName("Should return 409 when email is already registered during admin registration")
    void registerAdmin_DuplicateEmail_ShouldReturnConflict() throws Exception {
        when(adminSecret.getCode()).thenReturn("secret123");
        when(userService.superAdminExists()).thenReturn(false);
        when(userService.createSuperAdmin(any(UserRegistrationDto.class)))
                .thenThrow(new DuplicateEmailException("Email already registered"));

        mockMvc.perform(post("/api/v1/auth/register-admin")
                        .with(csrf())
                        .header("X-Admin-Secret", "secret123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered: test@example.com"));

        verify(userService, times(1)).createSuperAdmin(any(UserRegistrationDto.class));
    }

    @Test
    @DisplayName("Should login successfully and return JWT token")
    void login_Success_ShouldReturnToken() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(jwtUtil.generateToken(any(UserPrincipal.class), anyString())).thenReturn("mockJwtToken");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mockJwtToken"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.userId").value("user123"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.message").value("Login successful"));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, times(1)).generateToken(any(UserPrincipal.class), anyString());
    }

    @Test
    @DisplayName("Should throw exception when login fails")
    void login_InvalidCredentials_ShouldThrowException() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isInternalServerError());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
