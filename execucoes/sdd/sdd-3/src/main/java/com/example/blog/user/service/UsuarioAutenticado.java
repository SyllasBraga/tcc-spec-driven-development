package com.example.blog.user.service;

/**
 * Dados do usuário devolvidos após cadastro, login, consulta ou atualização, com um token válido.
 *
 * @param bio pode ser {@code null}
 * @param image pode ser {@code null}
 */
public record UsuarioAutenticado(String email, String token, String username, String bio, String image) {
}
