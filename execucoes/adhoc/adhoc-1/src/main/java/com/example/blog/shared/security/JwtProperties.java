package com.example.blog.shared.security;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração dos tokens JWT. O segredo vem do ambiente ({@code JWT_SECRET}) e precisa de ao menos 32 caracteres
 * para o algoritmo HS256.
 */
@Validated
@ConfigurationProperties("conduit.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @NotBlank String issuer,
        @NotNull Duration timeToLive) {
}
