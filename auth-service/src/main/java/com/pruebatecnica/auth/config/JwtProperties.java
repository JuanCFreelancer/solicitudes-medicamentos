package com.pruebatecnica.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configuración del token JWT (prefijo {@code app.jwt}). */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "JWT_SECRET debe tener al menos 32 caracteres") String secret,
        @Positive long expirationMinutes) {
}
