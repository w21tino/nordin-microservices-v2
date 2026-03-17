package com.nordin.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Gestión de Refresh Tokens en Redis.
 *
 * Estructura en Redis:
 *   Key:   "refresh:{token_opaco}"
 *   Value: "{userId}"
 *   TTL:   7 días (configurable)
 *
 * Al hacer logout o refresh, el token se elimina de Redis (revocación inmediata).
 * Un token no presente en Redis es inválido aunque no haya expirado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * Genera un refresh token opaco, lo almacena en Redis y retorna el token.
     */
    public String createRefreshToken(String userId) {
        String token = UUID.randomUUID().toString();
        String key = REFRESH_PREFIX + token;
        Duration ttl = Duration.ofMillis(refreshTokenExpiration);

        redisTemplate.opsForValue().set(key, userId, ttl);
        log.debug("Refresh token creado para userId: {}", userId);

        return token;
    }

    /**
     * Valida el refresh token — retorna el userId si es válido, null si no existe o expiró.
     */
    public String validateRefreshToken(String token) {
        String key = REFRESH_PREFIX + token;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Revoca el refresh token (logout o rotación).
     */
    public void revokeRefreshToken(String token) {
        String key = REFRESH_PREFIX + token;
        Boolean deleted = redisTemplate.delete(key);
        log.debug("Refresh token revocado: {}", deleted);
    }

    /**
     * Revoca todos los refresh tokens de un usuario.
     * Útil para "cerrar todas las sesiones".
     */
    public void revokeAllUserTokens(String userId) {
        // Scan de keys por patrón — solo usar en casos específicos, no en hot path
        redisTemplate.keys(REFRESH_PREFIX + "*").forEach(key -> {
            String storedUserId = redisTemplate.opsForValue().get(key);
            if (userId.equals(storedUserId)) {
                redisTemplate.delete(key);
            }
        });
        log.info("Todos los tokens revocados para userId: {}", userId);
    }
}
