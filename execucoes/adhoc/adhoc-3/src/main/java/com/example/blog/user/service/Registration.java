package com.example.blog.user.service;

import java.util.Objects;

/** Dados de cadastro já validados na borda da API. */
public record Registration(String username, String email, String password) {

    public Registration {
        Objects.requireNonNull(username);
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);
    }
}
