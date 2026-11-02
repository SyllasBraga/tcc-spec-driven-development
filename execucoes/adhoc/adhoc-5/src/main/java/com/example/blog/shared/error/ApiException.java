package com.example.blog.shared.error;

import java.util.Objects;

import org.springframework.http.HttpStatus;

/**
 * Erro de negócio convertido em resposta HTTP pelo {@link ApiExceptionHandler}.
 * O par {@code field}/{@code detail} vira {@code {"errors": {field: [detail]}}}.
 */
public abstract sealed class ApiException extends RuntimeException
        permits NotFoundException, ConflictException, UnauthorizedException, UnprocessableException {

    private final String field;
    private final String detail;

    protected ApiException(String field, String detail) {
        super(field + " " + detail);
        this.field = Objects.requireNonNull(field, "field");
        this.detail = Objects.requireNonNull(detail, "detail");
    }

    public abstract HttpStatus status();

    public String field() {
        return field;
    }

    public String detail() {
        return detail;
    }
}
