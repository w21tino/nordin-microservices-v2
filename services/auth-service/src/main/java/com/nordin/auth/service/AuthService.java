package com.nordin.auth.service;

import com.nordin.auth.dto.AuthDtos.*;
import com.nordin.auth.model.Role;
import com.nordin.auth.model.User;
import com.nordin.auth.repository.UserRepository;
import com.nordin.auth.security.JwtService;
import com.nordin.auth.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra un nuevo usuario.
     * El password se almacena con bcrypt.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El email ya está registrado: " + request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_USER)
                .build();

        User saved = userRepository.save(user);
        log.info("Usuario registrado: {}", saved.getEmail());

        return buildAuthResponse(saved);
    }

    /**
     * Login con email/password.
     * Retorna access token (JWT, 15min) + refresh token (opaco, Redis, 7 días).
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!user.isActive()) {
            throw new IllegalStateException("Usuario inactivo");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        log.info("Login exitoso: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    /**
     * Genera nuevo access token a partir de un refresh token válido.
     * Implementa rotación: el refresh token viejo se revoca y se emite uno nuevo.
     */
    @Transactional
    public AccessTokenResponse refresh(RefreshRequest request) {
        String userId = refreshTokenService.validateRefreshToken(request.refreshToken());

        if (userId == null) {
            throw new IllegalArgumentException("Refresh token inválido o expirado");
        }

        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!user.isActive()) {
            refreshTokenService.revokeRefreshToken(request.refreshToken());
            throw new IllegalStateException("Usuario inactivo");
        }

        // Rotación: revocar el token viejo antes de emitir uno nuevo
        refreshTokenService.revokeRefreshToken(request.refreshToken());

        String newAccessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name()
        );

        log.debug("Access token renovado para userId: {}", userId);

        return AccessTokenResponse.of(newAccessToken, jwtService.getAccessTokenExpiration());
    }

    /**
     * Logout — revoca el refresh token en Redis.
     * El access token expira solo (15min) — no se puede revocar sin lista negra.
     */
    public void logout(LogoutRequest request) {
        refreshTokenService.revokeRefreshToken(request.refreshToken());
        log.info("Logout completado — refresh token revocado");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name()
        );

        String refreshToken = refreshTokenService.createRefreshToken(user.getId().toString());

        UserInfo userInfo = new UserInfo(
                user.getId().toString(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );

        return AuthResponse.of(accessToken, refreshToken,
                jwtService.getAccessTokenExpiration(), userInfo);
    }
}
