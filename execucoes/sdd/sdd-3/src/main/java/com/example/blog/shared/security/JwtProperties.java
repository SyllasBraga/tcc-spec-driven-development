package com.example.blog.shared.security;

import java.time.Duration;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração do JWT ({@code app.jwt.*}).
 *
 * @param secret chave HMAC em Base64, com ao menos 32 bytes; vem do ambiente, nunca de arquivo versionado
 * @param issuer emissor gravado e exigido no claim {@code iss}
 * @param ttl validade do token
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(@NotBlank String secret, @NotBlank String issuer, @NotNull Duration ttl) {

    private static final int TAMANHO_MINIMO_BYTES = 32;
    private static final String ALGORITMO_HMAC = "HmacSHA256";

    /**
     * Decodifica o segredo e devolve a chave HMAC-SHA256.
     *
     * @throws IllegalStateException se o segredo não for Base64 válido ou tiver menos de 32 bytes
     */
    public SecretKey chave() {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.jwt.secret deve estar em Base64", e);
        }
        if (bytes.length < TAMANHO_MINIMO_BYTES) {
            throw new IllegalStateException("app.jwt.secret deve ter ao menos 32 bytes");
        }
        return new SecretKeySpec(bytes, ALGORITMO_HMAC);
    }
}
