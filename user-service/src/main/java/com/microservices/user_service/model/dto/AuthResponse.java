package com.microservices.user_service.model.dto;

import com.microservices.user_service.model.Role;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String userId;
    private String email;
    private String name;
    private Set<Role> roles;
    private String message;
}