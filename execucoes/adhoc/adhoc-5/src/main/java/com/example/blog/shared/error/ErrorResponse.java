package com.example.blog.shared.error;

import java.util.List;
import java.util.Map;

/**
 * Corpo de erro do contrato da API: {@code {"errors": {"<campo>": ["<mensagem>"]}}}.
 */
public record ErrorResponse(Map<String, List<String>> errors) {

    public ErrorResponse {
        errors = Map.copyOf(errors);
    }

    public static ErrorResponse of(String field, String message) {
        return new ErrorResponse(Map.of(field, List.of(message)));
    }
}
