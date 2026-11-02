package com.example.blog.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração dos tokens JWT ({@code conduit.jwt.*}). O segredo vem do ambiente ({@code JWT_SECRET}).
 *
 * @param secret   chave HMAC-SHA256, com pelo menos 32 bytes em UTF-8
 * @param issuer   emissor gravado e exigido em todo token
 * @param validade tempo de vida do token
 */
@Validated
@ConfigurationProperties("conduit.jwt")
public record JwtProperties(@NotBlank String secret, @NotBlank String issuer, @NotNull Duration validade) {

    static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;

    public JwtProperties {
        if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO_BYTES) {
            throw new IllegalArgumentException(
                    "conduit.jwt.secret deve ter pelo menos " + TAMANHO_MINIMO_SEGREDO_BYTES + " bytes");
        }
    }
}
