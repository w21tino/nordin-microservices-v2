package com.nordin.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Servicio de generación y validación de JWT.
 *
 * Access Token: JWT firmado con HS256, contiene userId, email y role.
 * El Gateway valida este token en cada request — auth-service solo lo genera.
 */
@Slf4j
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    public String generateAccessToken(String userId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(userId)
                .claims(Map.of(
                    "email", email,
                    "role", role
                ))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateAndExtract(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            validateAndExtract(token);
            return true;
        } catch (Exception e) {
            log.debug("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
