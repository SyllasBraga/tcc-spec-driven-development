package com.example.blog.shared.error;

/**
 * Mensagens de erro do contrato da API, no formato {@code {"errors": {"campo": ["mensagem"]}}}.
 */
public final class ErrorMessages {

    public static final String BLANK = "can't be blank";
    public static final String INVALID = "is invalid";
    public static final String INVALID_CREDENTIALS = "invalid";
    public static final String TOO_SHORT = "is too short (minimum is {min} characters)";
    public static final String TOO_LONG = "is too long (maximum is {max} characters)";
    public static final String TAKEN = "has already been taken";
    public static final String NOT_FOUND = "not found";

    private ErrorMessages() {
    }
}
