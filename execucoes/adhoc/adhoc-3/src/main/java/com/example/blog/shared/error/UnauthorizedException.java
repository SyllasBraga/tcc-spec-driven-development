package com.example.blog.shared.error;

/** Falha de autenticação (HTTP 401), por exemplo credenciais inválidas no login. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String field, String message) {
        super(field, message);
    }
}
