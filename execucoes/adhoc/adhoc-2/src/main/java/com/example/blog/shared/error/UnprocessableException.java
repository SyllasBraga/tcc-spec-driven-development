package com.example.blog.shared.error;

/** Requisição válida na forma, mas que viola uma regra de negócio (HTTP 422). */
public non-sealed class UnprocessableException extends DomainException {

    public UnprocessableException(String key, String message) {
        super(key, message);
    }
}
