package com.example.blog.profile.service;

/** Perfil público de um usuário, do ponto de vista de quem consulta. */
public record ProfileResult(String username, String bio, String image, boolean following) {
}
