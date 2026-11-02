package com.example.blog.user.service;

/**
 * Dados do usuário autenticado acompanhados de um token válido.
 */
public record AuthenticatedUser(Long id, String email, String username, String bio, String image, String token) {

    @Override
    public String toString() {
        return "AuthenticatedUser[id=" + id + ", username=" + username + "]";
    }
}
