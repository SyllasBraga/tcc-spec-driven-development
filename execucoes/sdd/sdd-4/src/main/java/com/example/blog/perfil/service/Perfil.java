package com.example.blog.perfil.service;

/** Visão pública de um usuário, com {@code following} calculado em relação a quem consulta. */
public record Perfil(String username, String bio, String image, boolean following) {
}
