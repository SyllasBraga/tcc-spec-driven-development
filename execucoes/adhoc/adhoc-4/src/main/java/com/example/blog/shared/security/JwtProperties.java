package com.example.blog.shared.security;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração dos tokens JWT emitidos pela API.
 *
 * @param secret chave HMAC em Base64 (no mínimo 32 bytes decodificados), vinda do ambiente
 * @param issuer emissor gravado e exigido nos tokens
 * @param timeToLive validade de cada token
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(@NotBlank String secret, @NotBlank String issuer, @NotNull Duration timeToLive) {
}
