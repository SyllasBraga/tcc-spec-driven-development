package com.example.blog.shared.error;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Recurso inexistente (404).
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String key, String message) {
        super(key, message);
    }

    public NotFoundException(Map<String, List<String>> errors) {
        super(errors);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.NOT_FOUND;
    }
}
