package com.microservices.user_service.security;

import com.microservices.user_service.model.Role;
import com.microservices.user_service.model.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

@Slf4j
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Extract headers passed by API Gateway
        String userId = request.getHeader("X-User-Id");
        String userEmail = request.getHeader("X-User-Email");

        log.debug("Headers from Gateway - X-User-Id: {}, X-User-Email: {}", userId, userEmail);

        // If headers exist, populate SecurityContext
        if (userId != null && userEmail != null) {
            // Create UserPrincipal
            UserPrincipal userPrincipal = UserPrincipal.builder()
                    .userId(userId)
                    .email(userEmail)
                    .roles(Set.of(Role.USER))
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

            log.debug("✅ SecurityContext populated for user: {}", userEmail);
        } else {
            log.debug("No Gateway headers found, skipping SecurityContext population");
        }

        filterChain.doFilter(request, response);
    }
}