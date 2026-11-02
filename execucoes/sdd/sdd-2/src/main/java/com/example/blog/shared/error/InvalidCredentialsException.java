package com.example.blog.shared.error;

/**
 * E-mail ou senha não conferem. Convertida em {@code 401 credentials: invalid}, sem indicar qual campo falhou.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciais inválidas");
    }
}
