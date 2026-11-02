package com.example.blog.user.service;

/**
 * E-mail e senha informados no login.
 */
public record Credenciais(String email, String password) {

    /** Não expõe e-mail nem senha em logs. */
    @Override
    public String toString() {
        return "Credenciais[***]";
    }
}
