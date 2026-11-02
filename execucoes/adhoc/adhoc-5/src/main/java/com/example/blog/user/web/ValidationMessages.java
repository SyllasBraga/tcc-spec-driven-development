package com.example.blog.user.web;

/** Mensagens e padrões de validação compartilhados pelos DTOs de usuário, no estilo do contrato da API. */
final class ValidationMessages {

    static final String BLANK = "can't be blank";
    static final String INVALID = "is invalid";
    static final String USERNAME_TOO_LONG = "is too long (maximum is 50 characters)";
    static final String EMAIL_TOO_LONG = "is too long (maximum is 254 characters)";

    /** Sem espaços e sem barra, pois o nome de usuário aparece no caminho {@code /api/profiles/{username}}. */
    static final String USERNAME_PATTERN = "^[^\\s/]*$";
    static final String NOT_BLANK_PATTERN = "(?s).*\\S.*";
    /** Complementa o {@code @Email}, que aceita domínio sem ponto (por exemplo {@code a@b}). */
    static final String EMAIL_DOMAIN_PATTERN = ".+@[^@.\\s]+(\\.[^@.\\s]+)+";

    private ValidationMessages() {
    }
}
