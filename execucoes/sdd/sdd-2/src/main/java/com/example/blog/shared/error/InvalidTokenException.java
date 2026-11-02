package com.example.blog.shared.error;

/**
 * Token aceito pelo filtro de segurança, mas que não identifica um usuário válido.
 * Convertida em {@code 401 token: is invalid}.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String motivo) {
        super(motivo);
    }

    public InvalidTokenException(String motivo, Throwable causa) {
        super(motivo, causa);
    }
}
