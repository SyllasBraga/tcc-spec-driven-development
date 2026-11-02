package com.example.blog.shared.error;

import org.springframework.http.HttpStatus;

/** Valor único já utilizado por outro registro (409). */
public final class ConflictException extends ApiException {

    public ConflictException(String field) {
        super(field, "has already been taken");
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.CONFLICT;
    }
}
