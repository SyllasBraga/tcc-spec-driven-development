package com.example.blog.usuario.service;

/** Dados do usuário autenticado devolvidos pelo serviço, acompanhados do token de acesso. */
public record UsuarioAutenticado(String email, String token, String username, String bio, String image) {
}
