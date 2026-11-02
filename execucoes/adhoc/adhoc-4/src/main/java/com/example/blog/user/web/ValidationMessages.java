package com.example.blog.user.web;

/** Mensagens de validação no estilo do contrato da API Conduit. */
final class ValidationMessages {

    static final String BLANK = "can't be blank";
    static final String INVALID = "is invalid";
    static final String TOO_LONG = "is too long (maximum is {max} characters)";
    /** Aceita {@code null} (campo ausente), mas rejeita texto vazio ou só com espaços. */
    static final String NOT_BLANK_REGEX = "(?s).*\\S.*";

    private ValidationMessages() {
    }
}
