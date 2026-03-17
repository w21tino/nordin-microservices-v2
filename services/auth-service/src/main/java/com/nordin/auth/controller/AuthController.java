package com.nordin.auth.controller;

import com.nordin.auth.dto.AuthDtos.*;
import com.nordin.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registro, login, refresh token y logout")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Registrar nuevo usuario",
               description = "Crea un usuario con ROLE_USER. Retorna access + refresh token.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario registrado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o email ya registrado")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registro de usuario: {}", request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Login",
               description = "Autentica con email/password. Retorna access token (15min) + refresh token (7 días).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso"),
        @ApiResponse(responseCode = "400", description = "Credenciales inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login: {}", request.email());
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Renovar access token",
               description = "Intercambia un refresh token válido por un nuevo access token. "
                           + "Implementa rotación: el refresh token viejo se revoca y se emite uno nuevo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Access token renovado"),
        @ApiResponse(responseCode = "400", description = "Refresh token inválido o expirado")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(summary = "Logout",
               description = "Revoca el refresh token en Redis. "
                           + "El access token expira naturalmente (máximo 15min).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout exitoso"),
        @ApiResponse(responseCode = "400", description = "Refresh token requerido")
    })
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(new MessageResponse("Sesión cerrada correctamente"));
    }
}
