package com.example.blog.usuario.service;

/**
 * Usuário autenticado com um token recém-emitido. Nunca carrega a senha nem o hash.
 */
public record UsuarioAutenticadoDto(Long id, String email, String username, String bio, String imagem, String token) {
}
