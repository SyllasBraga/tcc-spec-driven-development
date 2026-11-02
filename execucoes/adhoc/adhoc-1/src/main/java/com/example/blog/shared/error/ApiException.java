package com.example.blog.shared.error;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Base das exceções que a API converte em respostas de erro no formato do contrato.
 */
public abstract class ApiException extends RuntimeException {

    private final transient Map<String, List<String>> errors;

    protected ApiException(String key, String message) {
        this(Map.of(key, List.of(message)));
    }

    protected ApiException(Map<String, List<String>> errors) {
        super(errors.toString());
        this.errors = Map.copyOf(errors);
    }

    public Map<String, List<String>> errors() {
        return errors;
    }

    public abstract HttpStatus status();
}
