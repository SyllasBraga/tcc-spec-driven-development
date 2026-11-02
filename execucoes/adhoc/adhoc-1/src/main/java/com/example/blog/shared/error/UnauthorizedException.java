package com.example.blog.shared.error;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Falha de autenticação (401).
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String key, String message) {
        super(key, message);
    }

    public UnauthorizedException(Map<String, List<String>> errors) {
        super(errors);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.UNAUTHORIZED;
    }
}
