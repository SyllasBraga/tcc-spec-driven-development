package com.example.blog.user.service;

/** Dados do usuário autenticado devolvidos pelas operações de conta, com o token de acesso. */
public record UserResult(String email, String token, String username, String bio, String image) {
}
