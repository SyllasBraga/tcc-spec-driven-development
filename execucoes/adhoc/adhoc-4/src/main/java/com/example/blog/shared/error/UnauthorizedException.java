package com.example.blog.shared.error;

import org.springframework.http.HttpStatus;

/** Credenciais ausentes ou inválidas (HTTP 401). */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String field, String detail) {
        super(HttpStatus.UNAUTHORIZED, field, detail);
    }
}
