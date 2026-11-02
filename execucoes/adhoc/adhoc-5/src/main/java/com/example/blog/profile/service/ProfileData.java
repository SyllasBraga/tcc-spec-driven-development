package com.example.blog.profile.service;

/** Perfil público de um usuário, do ponto de vista de quem consulta. */
public record ProfileData(String username, String bio, String image, boolean following) {
}
