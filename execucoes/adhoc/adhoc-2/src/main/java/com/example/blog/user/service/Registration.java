package com.example.blog.user.service;

/** Dados de cadastro de um novo usuário, já validados na borda. */
public record Registration(String username, String email, String password) {
}
