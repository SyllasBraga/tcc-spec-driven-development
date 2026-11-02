package com.example.blog.shared.error;

import java.util.List;
import java.util.Map;

/**
 * Violação de regra de validação, convertida em {@code 422}.
 */
public class ValidationException extends RuntimeException {

    private final ErrorResponse body;

    public ValidationException(String chave, String mensagem) {
        this(Map.of(chave, List.of(mensagem)));
    }

    public ValidationException(Map<String, List<String>> errors) {
        super("Erro de validação nos campos " + errors.keySet());
        this.body = ErrorResponse.of(errors);
    }

    public ErrorResponse body() {
        return body;
    }
}
