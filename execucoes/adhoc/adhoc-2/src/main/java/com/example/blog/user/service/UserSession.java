package com.example.blog.user.service;

/** Usuário recém-autenticado (cadastro ou login) e o token emitido para ele. */
public record UserSession(UserAccount account, String token) {
}
