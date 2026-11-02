package com.example.blog.shared.erro;

/** E-mail inexistente ou senha incorreta no login (HTTP 401), sem revelar qual dos dois. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("credenciais inválidas");
    }
}
