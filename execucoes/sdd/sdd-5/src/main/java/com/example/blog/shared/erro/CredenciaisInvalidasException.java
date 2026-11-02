package com.example.blog.shared.erro;

/**
 * E-mail inexistente ou senha incorreta no login (401). Não informa qual dos dois falhou.
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Credenciais inválidas");
    }
}
