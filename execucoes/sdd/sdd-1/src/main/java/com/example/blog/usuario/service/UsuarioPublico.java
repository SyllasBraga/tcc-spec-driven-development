package com.example.blog.usuario.service;

/**
 * Dados públicos de um usuário, expostos a outras funcionalidades sem revelar a entidade.
 */
public record UsuarioPublico(Long id, String username, String bio, String image) {
}
