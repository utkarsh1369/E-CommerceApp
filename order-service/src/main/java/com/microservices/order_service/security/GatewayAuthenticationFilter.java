package com.microservices.order_service.security;

import com.microservices.order_service.model.Role;
import com.microservices.order_service.model.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Extract headers passed by API Gateway
        String userId = request.getHeader("X-User-Id");
        String userEmail = request.getHeader("X-User-Email");
        String rolesHeader = request.getHeader("X-User-Roles");

        log.debug("Headers from Gateway - X-User-Id: {}, X-User-Email: {}, X-User-Roles: {}", userId, userEmail, rolesHeader);

        // If headers exist, populate SecurityContext
        if (userId != null && userEmail != null && rolesHeader != null) {
            // Create UserPrincipal
            Set<Role> roles = Arrays.stream(rolesHeader.split(","))
                    .map(roleName -> roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName)
                    .map(Role::valueOf)
                    .collect(Collectors.toSet());

            UserPrincipal userPrincipal = UserPrincipal.builder()
                    .userId(userId)
                    .email(userEmail)
                    .roles(roles)
                    .build();

            // Create authentication token
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userPrincipal,
                            null,
                            userPrincipal.getAuthorities()
                    );

            // Set in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("SecurityContext populated for user: {}", userEmail);
        } else {
            log.debug("No Gateway headers found, skipping SecurityContext population");
        }

        filterChain.doFilter(request, response);
    }
}