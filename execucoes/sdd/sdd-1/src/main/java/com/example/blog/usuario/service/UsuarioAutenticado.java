package com.example.blog.usuario.service;

/**
 * Dados do usuário autenticado devolvidos pelo serviço, com o token JWT.
 */
public record UsuarioAutenticado(String email, String token, String username, String bio, String image) {
}
