package com.example.blog.shared.seguranca;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração dos tokens JWT emitidos pela API.
 *
 * @param secret chave HMAC-SHA256 em Base64, com no mínimo 32 bytes; vem da variável {@code JWT_SECRET}
 * @param issuer emissor gravado na claim {@code iss} e exigido na validação
 * @param ttl validade do token a partir da emissão
 */
@Validated
@ConfigurationProperties("conduit.jwt")
public record JwtProperties(@NotBlank String secret, @NotBlank String issuer, @NotNull Duration ttl) {

}
