package com.example.blog.shared.web;

/**
 * Credenciais de login inválidas, convertidas em {@code 401} sem indicar qual dado falhou.
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Credenciais inválidas");
    }
}
