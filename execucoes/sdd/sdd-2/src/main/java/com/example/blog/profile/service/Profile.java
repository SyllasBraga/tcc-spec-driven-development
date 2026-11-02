package com.example.blog.profile.service;

/**
 * Visão pública de um usuário, com {@code following} calculado em relação a quem consulta.
 */
public record Profile(String username, String bio, String image, boolean following) {
}
