package com.example.blog.profile.service;

/**
 * Visão pública de um usuário, com o indicador {@code following} relativo a quem consulta.
 *
 * @param bio pode ser {@code null}
 * @param image pode ser {@code null}
 */
public record Perfil(String username, String bio, String image, boolean following) {
}
