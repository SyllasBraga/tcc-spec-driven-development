package com.example.blog.perfil.web;

/**
 * Objeto {@code Profile} do contrato. {@code bio} e {@code image} nulos são serializados como {@code null}.
 */
public record PerfilResponse(String username, String bio, String image, boolean following) {
}
