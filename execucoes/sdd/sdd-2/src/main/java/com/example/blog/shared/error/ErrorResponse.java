package com.example.blog.shared.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Corpo de erro do contrato: {@code {"errors":{"<chave>":["<mensagem>", ...]}}}.
 * A ordem de inserção das chaves é preservada.
 */
public record ErrorResponse(Map<String, List<String>> errors) {

    public ErrorResponse {
        Objects.requireNonNull(errors, "errors");
        Map<String, List<String>> copia = new LinkedHashMap<>();
        errors.forEach((chave, mensagens) -> copia.put(chave, List.copyOf(mensagens)));
        errors = Collections.unmodifiableMap(copia);
    }

    public static ErrorResponse of(String chave, String mensagem) {
        return new ErrorResponse(Map.of(chave, List.of(mensagem)));
    }

    public static ErrorResponse of(Map<String, List<String>> errors) {
        return new ErrorResponse(errors);
    }
}
