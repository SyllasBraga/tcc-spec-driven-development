package com.example.blog.usuario.service;

/** Dados públicos de um usuário, usados para montar o perfil. */
public record DadosPublicosUsuario(long id, String username, String bio, String image) {
}
