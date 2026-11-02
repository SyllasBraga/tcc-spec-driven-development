package com.example.blog.shared.erro;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.MediaType;

/** Corpo de erro do contrato: {@code {"errors": {"<chave>": ["<mensagem>", ...]}}}. */
public record ErroResponse(Map<String, List<String>> errors) {

    /** Tipo de conteúdo de toda resposta de erro: {@code application/json;charset=UTF-8}. */
    public static final MediaType TIPO_DE_CONTEUDO = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

    public ErroResponse {
        Objects.requireNonNull(errors, "errors");
        Map<String, List<String>> copia = new LinkedHashMap<>();
        errors.forEach((chave, mensagens) -> copia.put(chave, List.copyOf(mensagens)));
        errors = Collections.unmodifiableMap(copia);
    }

    public static ErroResponse de(String chave, String mensagem) {
        return new ErroResponse(Map.of(chave, List.of(mensagem)));
    }
}
