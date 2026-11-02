package com.example.blog.user.service;

import java.util.Objects;

/** Dados de cadastro de um novo usuário, já validados na borda. */
public record RegisterUserCommand(String username, String email, String password) {

    public RegisterUserCommand {
        Objects.requireNonNull(username);
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);
    }
}
