package com.example.blog.shared.error;

/** Mensagens de validação no estilo do contrato Conduit, para uso em anotações de Bean Validation. */
public final class ValidationMessages {

    public static final String BLANK = "can't be blank";
    public static final String INVALID = "is invalid";
    public static final String TOO_LONG = "is too long (maximum is {max} characters)";

    /** Ao menos um caractere que não seja espaço; {@code null} é aceito (use com campos opcionais). */
    public static final String NOT_BLANK_PATTERN = "(?s).*\\S.*";

    private ValidationMessages() {
    }
}
