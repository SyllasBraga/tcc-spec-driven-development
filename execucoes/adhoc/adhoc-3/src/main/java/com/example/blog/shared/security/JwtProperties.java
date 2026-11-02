package com.example.blog.shared.security;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração dos tokens JWT emitidos pela API.
 *
 * @param secret     chave HMAC (HS256); vem do ambiente ({@code JWT_SECRET}), com pelo menos 32 caracteres
 * @param issuer     emissor gravado e exigido no claim {@code iss}
 * @param timeToLive validade de cada token
 */
@Validated
@ConfigurationProperties("conduit.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @NotBlank String issuer,
        @NotNull Duration timeToLive) {
}
