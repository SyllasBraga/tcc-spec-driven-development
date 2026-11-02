package com.example.blog.shared.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração do JWT ({@code app.jwt.*}). O segredo vem da variável de ambiente {@code JWT_SECRET}.
 *
 * @param secret segredo HMAC com pelo menos 32 caracteres
 * @param issuer emissor gravado e exigido no claim {@code iss}
 * @param ttl    validade do token a partir da emissão
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @NotBlank String issuer,
        @NotNull Duration ttl) {

    @Override
    public String toString() {
        return "JwtProperties[secret=***, issuer=" + issuer + ", ttl=" + ttl + "]";
    }
}
