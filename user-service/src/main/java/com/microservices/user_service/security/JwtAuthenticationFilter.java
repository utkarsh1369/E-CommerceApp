package com.microservices.user_service.security;

import com.microservices.user_service.model.Role;
import com.microservices.user_service.model.UserPrincipal;
import com.microservices.user_service.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // ===== PRIORITY 1: Check for Gateway Headers (Fast Path) =====
        String gatewayUserId = request.getHeader("X-User-Id");
        String gatewayUserEmail = request.getHeader("X-User-Email");

        if (gatewayUserId != null && gatewayUserEmail != null) {
            log.debug("🚪 Request from API Gateway - User ID: {}, Email: {}", gatewayUserId, gatewayUserEmail);

            UserPrincipal userPrincipal = UserPrincipal.builder()
                    .userId(gatewayUserId)
                    .email(gatewayUserEmail)
                    .roles(Set.of(Role.USER))
                    .build();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userPrincipal,
                            null,
                            userPrincipal.getAuthorities()
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("✅ SecurityContext populated from Gateway headers for user: {}", gatewayUserEmail);
            filterChain.doFilter(request, response);
            return; // Skip JWT validation - Gateway already did it!
        }

        // ===== PRIORITY 2: Direct Request (Fallback to JWT Validation) =====
        log.debug("🔐 Direct request detected - validating JWT token");

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String email = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                email = jwtUtil.extractUsername(token);
                log.debug("Extracted email from JWT: {}", email);
            } catch (Exception e) {
                log.error("JWT token extraction failed: {}", e.getMessage());
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("Loading user details for email: {}", email);
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

            if (jwtUtil.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("✅ User {} authenticated with roles: {}", email, userDetails.getAuthorities());
            } else {
                log.warn("❌ Token validation failed for user: {}", email);
            }
        }

        filterChain.doFilter(request, response);
    }
}