package com.example.blog.shared.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Corpo de erro do contrato da API: {@code {"errors": {"<campo>": ["<mensagem>", ...]}}}.
 *
 * @param errors mensagens agrupadas pelo campo ou recurso que causou o erro
 */
public record ErrorResponse(Map<String, List<String>> errors) {

    public ErrorResponse {
        // Cópia que preserva a ordem de inserção, para respostas determinísticas.
        Map<String, List<String>> copy = new LinkedHashMap<>();
        errors.forEach((field, messages) -> copy.put(field, List.copyOf(messages)));
        errors = Collections.unmodifiableMap(copy);
    }

    public static ErrorResponse of(String field, String detail) {
        return new ErrorResponse(Map.of(field, List.of(detail)));
    }
}
