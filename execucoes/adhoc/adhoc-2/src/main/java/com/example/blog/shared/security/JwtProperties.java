package com.example.blog.shared.security;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração dos tokens JWT.
 *
 * @param secret chave HMAC (HS256) com no mínimo 32 caracteres, vinda do ambiente
 * @param issuer emissor gravado e validado nos tokens
 * @param expiration validade de cada token emitido
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @NotBlank String issuer,
        @NotNull Duration expiration) {
}
