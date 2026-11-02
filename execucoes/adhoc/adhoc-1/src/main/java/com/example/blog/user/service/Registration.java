package com.example.blog.user.service;

import java.util.Objects;

/**
 * Dados validados para cadastrar um usuário.
 */
public record Registration(String username, String email, String password) {

    public Registration {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(password, "password");
    }
}
