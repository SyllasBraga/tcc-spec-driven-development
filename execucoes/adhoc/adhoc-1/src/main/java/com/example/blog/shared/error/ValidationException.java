package com.example.blog.shared.error;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Requisição que viola uma regra de validação (422).
 */
public class ValidationException extends ApiException {

    public ValidationException(String key, String message) {
        super(key, message);
    }

    public ValidationException(Map<String, List<String>> errors) {
        super(errors);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.UNPROCESSABLE_CONTENT;
    }
}
