package com.example.blog.shared.security;

/**
 * Token com assinatura válida que aponta para um usuário inexistente, convertido em {@code 401}.
 */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException() {
        super("Token não corresponde a um usuário existente");
    }
}
