package com.example.blog.user.service;

/**
 * Dados de cadastro já validados na borda.
 */
public record NovoUsuario(String username, String email, String password) {

    /** Não expõe a senha em logs. */
    @Override
    public String toString() {
        return "NovoUsuario[username=" + username + "]";
    }
}
