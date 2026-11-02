package com.example.blog.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração dos tokens JWT emitidos pela API.
 *
 * @param secret     segredo HMAC (HS256) com pelo menos 32 bytes, vindo do ambiente
 * @param issuer     emissor gravado e exigido na claim {@code iss}
 * @param timeToLive validade de cada token
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(@NotBlank String secret, @NotBlank String issuer, @NotNull Duration timeToLive) {

    private static final int MINIMO_BYTES_SEGREDO = 32;

    public JwtProperties {
        if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length < MINIMO_BYTES_SEGREDO) {
            throw new IllegalArgumentException(
                    "app.jwt.secret deve ter pelo menos " + MINIMO_BYTES_SEGREDO + " bytes");
        }
    }
}
