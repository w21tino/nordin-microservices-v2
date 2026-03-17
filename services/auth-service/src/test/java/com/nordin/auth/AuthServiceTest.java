package com.nordin.auth;

import com.nordin.auth.dto.AuthDtos.*;
import com.nordin.auth.model.Role;
import com.nordin.auth.model.User;
import com.nordin.auth.repository.UserRepository;
import com.nordin.auth.security.JwtService;
import com.nordin.auth.security.RefreshTokenService;
import com.nordin.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AuthService authService;

    private User testUser;
    private final String TEST_EMAIL = "test@nordin.com";
    private final String TEST_PASSWORD = "password123";
    private final UUID TEST_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_ID)
                .name("Test User")
                .email(TEST_EMAIL)
                .password("encoded_password")
                .role(Role.ROLE_USER)
                .active(true)
                .build();

        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access.token.jwt");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh-token-uuid");
    }

    // ─── Register ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register → exitoso → retorna tokens")
    void register_success() {
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AuthResponse response = authService.register(
                new RegisterRequest("Test User", TEST_EMAIL, TEST_PASSWORD));

        assertThat(response.accessToken()).isEqualTo("access.token.jwt");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-uuid");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo(TEST_EMAIL);

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(TEST_PASSWORD);
    }

    @Test
    @DisplayName("register → email duplicado → lanza IllegalArgumentException")
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        assertThatThrownBy(() ->
                authService.register(new RegisterRequest("Test", TEST_EMAIL, TEST_PASSWORD)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está registrado");

        verify(userRepository, never()).save(any());
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login → credenciales correctas → retorna tokens")
    void login_success() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, "encoded_password")).thenReturn(true);

        AuthResponse response = authService.login(new LoginRequest(TEST_EMAIL, TEST_PASSWORD));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("login → email no existe → lanza IllegalArgumentException")
    void login_emailNotFound_throws() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(TEST_EMAIL, TEST_PASSWORD)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenciales inválidas");
    }

    @Test
    @DisplayName("login → password incorrecto → lanza IllegalArgumentException")
    void login_wrongPassword_throws() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(TEST_EMAIL, "wrong")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenciales inválidas");
    }

    @Test
    @DisplayName("login → usuario inactivo → lanza IllegalStateException")
    void login_inactiveUser_throws() {
        testUser = User.builder()
                .id(TEST_ID).email(TEST_EMAIL).password("encoded")
                .role(Role.ROLE_USER).active(false).build();
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(new LoginRequest(TEST_EMAIL, TEST_PASSWORD)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inactivo");
    }

    // ─── Refresh ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("refresh → token válido → retorna nuevo access token y revoca el viejo")
    void refresh_success() {
        when(refreshTokenService.validateRefreshToken("old-refresh-token"))
                .thenReturn(TEST_ID.toString());
        when(userRepository.findById(TEST_ID)).thenReturn(Optional.of(testUser));

        AccessTokenResponse response = authService.refresh(new RefreshRequest("old-refresh-token"));

        assertThat(response.accessToken()).isEqualTo("access.token.jwt");
        assertThat(response.tokenType()).isEqualTo("Bearer");

        // Verifica rotación: el token viejo debe revocarse
        verify(refreshTokenService).revokeRefreshToken("old-refresh-token");
    }

    @Test
    @DisplayName("refresh → token inválido → lanza IllegalArgumentException")
    void refresh_invalidToken_throws() {
        when(refreshTokenService.validateRefreshToken("invalid")).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("invalid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválido o expirado");
    }

    // ─── Logout ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("logout → revoca refresh token en Redis")
    void logout_revokesToken() {
        authService.logout(new LogoutRequest("refresh-token-to-revoke"));

        verify(refreshTokenService).revokeRefreshToken("refresh-token-to-revoke");
    }
}
