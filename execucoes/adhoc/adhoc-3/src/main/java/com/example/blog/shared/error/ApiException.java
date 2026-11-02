package com.example.blog.shared.error;

import java.util.Objects;

/**
 * Base das exceções de domínio convertidas em respostas de erro da API.
 *
 * <p>Cada exceção carrega a chave e a mensagem que aparecem no corpo de erro
 * {@code {"errors": {"<chave>": ["<mensagem>"]}}}. O status HTTP é decidido
 * pelo {@link GlobalExceptionHandler} conforme o tipo concreto.
 */
public abstract class ApiException extends RuntimeException {

    private final String errorKey;
    private final String errorMessage;

    protected ApiException(String errorKey, String errorMessage) {
        super(errorKey + " " + errorMessage);
        this.errorKey = Objects.requireNonNull(errorKey);
        this.errorMessage = Objects.requireNonNull(errorMessage);
    }

    public String getErrorKey() {
        return errorKey;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
