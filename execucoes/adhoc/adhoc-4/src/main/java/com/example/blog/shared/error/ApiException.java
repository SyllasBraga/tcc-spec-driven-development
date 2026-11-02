package com.example.blog.shared.error;

import java.util.Objects;

import org.springframework.http.HttpStatus;

/**
 * Base das exceções de domínio convertidas em resposta HTTP no formato de erro do contrato
 * ({@code {"errors": {"<campo>": ["<mensagem>"]}}}).
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String field;
    private final String detail;

    protected ApiException(HttpStatus status, String field, String detail) {
        super(field + " " + detail);
        this.status = Objects.requireNonNull(status);
        this.field = Objects.requireNonNull(field);
        this.detail = Objects.requireNonNull(detail);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getField() {
        return field;
    }

    public String getDetail() {
        return detail;
    }
}
