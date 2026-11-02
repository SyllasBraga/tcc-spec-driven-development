package com.example.blog.shared.error;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Recurso que já existe (409).
 */
public class ConflictException extends ApiException {

    public ConflictException(String key, String message) {
        super(key, message);
    }

    public ConflictException(Map<String, List<String>> errors) {
        super(errors);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.CONFLICT;
    }
}
