package com.nordin.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// ─── Request DTOs ─────────────────────────────────────────────────────────────

public class AuthDtos {

    public record RegisterRequest(
        @NotBlank(message = "El nombre es requerido")
        String name,

        @NotBlank(message = "El email es requerido")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "La contraseña es requerida")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password
    ) {}

    public record LoginRequest(
        @NotBlank(message = "El email es requerido")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "La contraseña es requerida")
        String password
    ) {}

    public record RefreshRequest(
        @NotBlank(message = "El refresh token es requerido")
        String refreshToken
    ) {}

    public record LogoutRequest(
        @NotBlank(message = "El refresh token es requerido")
        String refreshToken
    ) {}

    // ─── Response DTOs ─────────────────────────────────────────────────────────

    public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserInfo user
    ) {
        public static AuthResponse of(String accessToken, String refreshToken,
                                      long expiresIn, UserInfo user) {
            return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
        }
    }

    public record AccessTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
    ) {
        public static AccessTokenResponse of(String accessToken, long expiresIn) {
            return new AccessTokenResponse(accessToken, "Bearer", expiresIn);
        }
    }

    public record UserInfo(
        String id,
        String name,
        String email,
        String role
    ) {}

    public record MessageResponse(String message) {}
}
