package com.microservices.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtUtil {

    private String secret;
    private Long expiration;

    private SecretKey getSigningKey() {
        log.debug("Using JWT secret (first 10 chars): {}...",
                secret != null ? secret.substring(0, Math.min(10, secret.length())) : "null");
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            log.debug("Attempting to parse JWT token...");
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.info(" Token parsed successfully. Subject: {}, UserId: {}",
                    claims.getSubject(), claims.get("userId"));
            return claims;

        } catch (SignatureException e) {
            log.error(" Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error(" Malformed JWT token: {}", e.getMessage());
            throw e;
        } catch (ExpiredJwtException e) {
            log.error(" Expired JWT token: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("JWT parsing error: {}", e.getMessage(), e);
            throw e;
        }
    }

    private Boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        Date now = new Date();

        log.debug("Token expiration: {}, Current time: {}", expiration, now);

        boolean expired = expiration.before(now);
        if (expired) {
            log.warn(" Token is EXPIRED");
        }

        return expired;
    }


    public Boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);

            log.info(" Token is VALID");
            return true;

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}