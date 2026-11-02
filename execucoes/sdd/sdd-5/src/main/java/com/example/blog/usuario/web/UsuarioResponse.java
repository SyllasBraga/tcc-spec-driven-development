package com.example.blog.usuario.web;

/**
 * Objeto {@code User} do contrato. {@code bio} e {@code image} nulos são serializados como {@code null}.
 */
public record UsuarioResponse(String email, String token, String username, String bio, String image) {
}
