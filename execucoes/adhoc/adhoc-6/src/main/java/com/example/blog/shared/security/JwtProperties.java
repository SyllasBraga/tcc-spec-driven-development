package com.example.blog.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT settings. The secret comes from the environment ({@code JWT_SECRET}) and is used for HS256 signatures.
 *
 * @param secret HMAC key, at least 32 bytes
 * @param issuer value of the {@code iss} claim, validated on every request
 * @param ttl token lifetime
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(@NotBlank String secret, @NotBlank String issuer, @NotNull Duration ttl) {

    static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "app.jwt.secret (JWT_SECRET) must have at least " + MIN_SECRET_BYTES + " bytes");
        }
        if (ttl != null && (ttl.isZero() || ttl.isNegative())) {
            throw new IllegalArgumentException("app.jwt.ttl must be positive");
        }
    }
}
