package com.example.blog.shared.error;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Input that is well formed but breaks a validation rule (HTTP 422).
 */
public class ValidationException extends ApiException {

    public ValidationException(String key, String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, key, message);
    }

    public ValidationException(Map<String, List<String>> errors) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, errors);
    }
}
