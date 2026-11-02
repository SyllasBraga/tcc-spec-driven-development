package com.example.blog.shared.erro;

/** Token válido na assinatura, mas que não identifica um usuário existente (HTTP 401). */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException() {
        super("token inválido");
    }
}
