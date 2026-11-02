package com.example.blog.shared.error;

import org.springframework.http.HttpStatus;

/** Requisição bem formada que viola uma regra de validação (422). */
public final class UnprocessableException extends ApiException {

    public UnprocessableException(String field, String detail) {
        super(field, detail);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.UNPROCESSABLE_CONTENT;
    }
}
