package com.microservices.user_service.controller;

import com.microservices.user_service.model.dto.AuthRequest;
import com.microservices.user_service.model.dto.AuthResponse;
import com.microservices.user_service.model.dto.UserRegistrationDto;
import com.microservices.user_service.model.dto.UserDto;
import com.microservices.user_service.model.UserPrincipal;
import com.microservices.user_service.service.UserService;
import com.microservices.user_service.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody UserRegistrationDto registrationDto) {
        log.info("Registration request received for email: {}", registrationDto.getEmail());
        UserDto userDto = userService.registerUser(registrationDto);
        return new ResponseEntity<>(userDto, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        log.info("Login request received for email: {}", authRequest.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getEmail(),
                            authRequest.getPassword()
                    )
            );
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            String token = jwtUtil.generateToken(userPrincipal, userPrincipal.getUserId());

            log.info("Login successful for user: {}", authRequest.getEmail());

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
            log.error("Login failed for email: {}", authRequest.getEmail());
            throw e;
        }
    }
}