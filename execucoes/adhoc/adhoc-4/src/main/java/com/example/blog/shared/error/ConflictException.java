package com.example.blog.shared.error;

import org.springframework.http.HttpStatus;

/** O recurso já existe (HTTP 409). */
public class ConflictException extends ApiException {

    public static final String ALREADY_TAKEN = "has already been taken";

    public ConflictException(String field) {
        super(HttpStatus.CONFLICT, field, ALREADY_TAKEN);
    }
}
