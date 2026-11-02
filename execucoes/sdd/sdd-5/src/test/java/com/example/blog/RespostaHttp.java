package com.example.blog;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/**
 * Resposta HTTP recebida nos testes de integração.
 *
 * @param status código de status
 * @param tipo cabeçalho {@code Content-Type} interpretado ({@code null} se ausente)
 * @param corpo corpo JSON ({@code MissingNode} se vazio)
 */
public record RespostaHttp(int status, MediaType tipo, JsonNode corpo) {

    /** Confere {@code application/json} com {@code charset=utf-8} (sem diferenciar maiúsculas). */
    public RespostaHttp comJsonUtf8() {
        assertThat(tipo).as("Content-Type").isNotNull();
        assertThat(tipo.isCompatibleWith(MediaType.APPLICATION_JSON)).as("tipo %s", tipo).isTrue();
        assertThat(tipo.getCharset()).as("charset de %s", tipo).isEqualTo(StandardCharsets.UTF_8);
        return this;
    }
}
