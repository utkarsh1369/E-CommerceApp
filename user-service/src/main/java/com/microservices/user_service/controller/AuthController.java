package com.microservices.user_service.controller;

import com.microservices.user_service.model.dto.AdminSecretProperties;
import com.microservices.user_service.exception.DuplicateEmailException;
import com.microservices.user_service.model.dto.*;
import com.microservices.user_service.model.UserPrincipal;
import com.microservices.user_service.service.UserService;
import com.microservices.user_service.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @Operation(summary = "Register new User", description = "Creates a new User in Database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created USER",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Bad Request (Invalid Input)", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    public ResponseEntity<UserDto> register(@io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User Registration Details",required = true)
                                                @Valid @RequestBody UserRegistrationDto registrationDto) {
        UserDto userDto = userService.registerUser(registrationDto);
        return new ResponseEntity<>(userDto, HttpStatus.CREATED);
    }

    @PostMapping("/register-admin")
    @Operation(summary = "Create a SUPER_ADMIN", description = "It creates a SUPER_ADMIN in Database only if there is no existing SUPER_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "SUPER_ADMIN Created",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessMessage.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Bad Request (Invalid Input)", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Invalid admin secret code", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "410", description = "Super Admin already exists", content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    public ResponseEntity<?> registerAdmin(@io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Admin Registration Details",required = true) @Valid @RequestBody UserRegistrationDto registrationDto,
                                           @Parameter(
                                                   description = "enter the already defined Admin Secret.",
                                                   required = true,
                                                   in = ParameterIn.HEADER,
                                                   example = "your-secret-key-123")
                                           @RequestHeader("X-Admin-Secret") String providedSecretCode) {
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
    @Operation(summary = "Login as a USER or ADMIN", description = "To login using email and password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Login Successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Bad Request (Invalid Input)", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Invalid email or password)", content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Login Using email and password.",required = true) @Valid @RequestBody AuthRequest authRequest) {
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
    }
}