package com.example.blog.shared.erro;

/**
 * Token sintaticamente válido, mas que não identifica um usuário existente (401).
 */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException() {
        super("Token não corresponde a um usuário válido");
    }
}
