package com.example.blog.shared.error;

import org.springframework.http.HttpStatus;

/** A requisição viola uma regra de validação ou de negócio (HTTP 422). */
public class UnprocessableEntityException extends ApiException {

    public UnprocessableEntityException(String field, String detail) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, field, detail);
    }
}
