package com.example.blog.perfil.service;

/**
 * Perfil público de um usuário, do ponto de vista de quem consulta.
 *
 * @param isFollowing se quem consulta segue o perfil
 */
public record Perfil(String username, String bio, String image, boolean isFollowing) {
}
