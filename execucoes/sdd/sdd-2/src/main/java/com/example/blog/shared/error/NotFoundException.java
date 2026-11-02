package com.example.blog.shared.error;

/**
 * Recurso inexistente, convertido em {@code 404} com {@code {recurso: ["not found"]}}.
 */
public class NotFoundException extends RuntimeException {

    private final ErrorResponse body;

    public NotFoundException(String recurso) {
        super("Recurso não encontrado: " + recurso);
        this.body = ErrorResponse.of(recurso, "not found");
    }

    public ErrorResponse body() {
        return body;
    }
}
