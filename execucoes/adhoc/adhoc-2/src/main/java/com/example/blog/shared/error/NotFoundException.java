package com.example.blog.shared.error;

/** Recurso inexistente (HTTP 404). */
public non-sealed class NotFoundException extends DomainException {

    public NotFoundException(String key, String message) {
        super(key, message);
    }
}
