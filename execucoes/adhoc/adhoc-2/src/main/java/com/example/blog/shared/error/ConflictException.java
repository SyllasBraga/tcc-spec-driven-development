package com.example.blog.shared.error;

/** Conflito com um recurso existente, por exemplo um valor único já usado (HTTP 409). */
public non-sealed class ConflictException extends DomainException {

    public ConflictException(String key, String message) {
        super(key, message);
    }
}
