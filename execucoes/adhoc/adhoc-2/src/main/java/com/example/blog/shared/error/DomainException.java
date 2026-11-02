package com.example.blog.shared.error;

/**
 * Base das exceções de domínio convertidas em resposta HTTP pelo {@link ApiExceptionHandler}.
 * A chave identifica o campo ou recurso afetado no corpo de erro.
 */
public abstract sealed class DomainException extends RuntimeException
        permits NotFoundException, ConflictException, UnprocessableException, UnauthenticatedException {

    private final String key;

    protected DomainException(String key, String message) {
        super(message);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
