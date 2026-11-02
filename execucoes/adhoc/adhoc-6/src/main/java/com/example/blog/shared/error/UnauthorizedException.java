package com.example.blog.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Authentication is missing or invalid (HTTP 401).
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String key, String message) {
        super(HttpStatus.UNAUTHORIZED, key, message);
    }
}
