package com.example.blog.perfil.service;

/**
 * Perfil público de um usuário, do ponto de vista de quem consulta.
 *
 * @param following {@code true} se quem consulta segue este usuário; sempre {@code false} para anônimos
 */
public record Perfil(String username, String bio, String image, boolean following) {

}
