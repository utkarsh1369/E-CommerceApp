package com.microservices.user_service.controller;

import com.microservices.user_service.model.dto.AdminSecretProperties;
import com.microservices.user_service.exception.DuplicateEmailException;
import com.microservices.user_service.model.dto.*;
import com.microservices.user_service.model.UserPrincipal;
import com.microservices.user_service.service.UserService;
import com.microservices.user_service.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authorization APIs")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final AdminSecretProperties adminSecret;


    @PostMapping("/register-user")
    @Operation(description = "To register a new user.")
    public ResponseEntity<UserDto> register(@Valid @RequestBody UserRegistrationDto registrationDto) {
        UserDto userDto = userService.registerUser(registrationDto);
        return new ResponseEntity<>(userDto, HttpStatus.CREATED);
    }

    @PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody UserRegistrationDto registrationDto, @RequestHeader("X-Admin-Secret") String providedSecretCode, HttpServletRequest request) {
        if (!adminSecret.getCode().equals(providedSecretCode)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorMessage("Invalid admin secret code"));
        }
        try {
            if (userService.superAdminExists()) {
                return ResponseEntity.status(HttpStatus.GONE)
                        .body(new ErrorMessage("Super Admin already exists. This endpoint is now permanently disabled for security."));
            }
            UserDto superAdmin = userService.createSuperAdmin(registrationDto);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new SuccessMessage("Super Admin created successfully with email: " + superAdmin.getEmail() +
                            ". This endpoint is now disabled for security."));

        } catch (DuplicateEmailException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorMessage("Email already registered: " + registrationDto.getEmail()));

        }
    }

    @PostMapping("/login")
    @Operation(description = "To login using email and password.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getEmail(),
                            authRequest.getPassword()
                    )
            );
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userPrincipal, userPrincipal.getUserId());

            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .userId(userPrincipal.getUserId())
                    .email(userPrincipal.getEmail())
                    .name(userPrincipal.getUsername())
                    .roles(userPrincipal.getRoles())
                    .message("Login successful")
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }
}