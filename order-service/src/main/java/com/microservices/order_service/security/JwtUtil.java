package com.microservices.order_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtUtil {

    private String secret;
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }
    public Map<String, Object> extractUserDetails(String token) {
        Claims claims = extractAllClaims(token);

        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("email", claims.getSubject());
        userDetails.put("userId", claims.get("userId", String.class));
        userDetails.put("roles", claims.get("roles", List.class));

        return userDetails;
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Collection<? extends GrantedAuthority> getAuthorities(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object rolesObject = claims.get("roles");

            if (rolesObject instanceof List<?> roles) {
                return roles.stream()
                        .filter(role -> role instanceof String)
                        .map(role -> new SimpleGrantedAuthority((String) role))
                        .collect(Collectors.toList());
            }
            log.warn("No roles found in token, returning empty authorities");
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("Error extracting authorities from token: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public Boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

}
