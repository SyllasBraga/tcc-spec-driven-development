package com.example.blog.shared.error;

import java.util.List;
import java.util.Map;

/**
 * Corpo de erro do contrato da API: cada chave identifica o campo ou recurso e aponta para as mensagens.
 */
public record ErrorsResponse(Map<String, List<String>> errors) {

    public ErrorsResponse {
        errors = Map.copyOf(errors);
    }

    public static ErrorsResponse of(String key, String message) {
        return new ErrorsResponse(Map.of(key, List.of(message)));
    }
}
