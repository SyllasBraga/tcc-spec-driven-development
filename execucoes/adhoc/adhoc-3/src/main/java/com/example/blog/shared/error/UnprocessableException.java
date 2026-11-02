package com.example.blog.shared.error;

/** Requisição bem formada que viola uma regra de validação ou de negócio (HTTP 422). */
public class UnprocessableException extends ApiException {

    public UnprocessableException(String field, String message) {
        super(field, message);
    }
}
