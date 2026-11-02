package com.example.blog.shared.seguranca;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração dos tokens de acesso. O segredo vem do ambiente ({@code JWT_SECRET}) e precisa ter
 * ao menos 32 caracteres (256 bits) para o HS256.
 *
 * @param segredo chave simétrica de assinatura
 * @param validade tempo de vida do token
 * @param emissor valor da claim {@code iss}
 */
@Validated
@ConfigurationProperties(prefix = "conduit.token")
public record TokenProperties(
        @NotBlank @Size(min = 32) String segredo,
        @NotNull Duration validade,
        @NotBlank String emissor) {
}
