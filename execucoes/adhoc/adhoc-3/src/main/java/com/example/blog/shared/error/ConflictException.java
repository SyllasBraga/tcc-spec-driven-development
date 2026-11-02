package com.example.blog.shared.error;

/** Conflito com um recurso já existente (HTTP 409), por exemplo e-mail já cadastrado. */
public class ConflictException extends ApiException {

    public ConflictException(String field, String message) {
        super(field, message);
    }
}
