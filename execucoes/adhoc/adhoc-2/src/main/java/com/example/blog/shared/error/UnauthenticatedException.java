package com.example.blog.shared.error;

/** Falha de autenticação, por exemplo credenciais inválidas (HTTP 401). */
public non-sealed class UnauthenticatedException extends DomainException {

    public UnauthenticatedException(String key, String message) {
        super(key, message);
    }
}
