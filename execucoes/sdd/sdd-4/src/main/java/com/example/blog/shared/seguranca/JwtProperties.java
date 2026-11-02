package com.example.blog.shared.seguranca;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração dos tokens JWT ({@code conduit.jwt.*}).
 *
 * @param segredo chave HS256, com pelo menos 32 bytes em UTF-8; vem da variável de ambiente {@code JWT_SECRET}
 * @param validade tempo de vida do token
 * @param emissor valor da claim {@code iss}
 */
@Validated
@ConfigurationProperties("conduit.jwt")
public record JwtProperties(@NotBlank String segredo, @NotNull Duration validade, @NotBlank String emissor) {

    static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;

    public JwtProperties {
        if (segredo != null && segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO_BYTES) {
            throw new IllegalArgumentException(
                    "conduit.jwt.segredo deve ter pelo menos " + TAMANHO_MINIMO_SEGREDO_BYTES + " bytes");
        }
    }

    byte[] segredoEmBytes() {
        return segredo.getBytes(StandardCharsets.UTF_8);
    }
}
