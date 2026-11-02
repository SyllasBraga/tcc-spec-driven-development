package com.example.blog.usuario.web;

import com.example.blog.usuario.service.UsuarioAutenticado;

/**
 * Corpo User do contrato: {@code {"user": {"email", "token", "username", "bio", "image"}}}.
 * {@code bio} e {@code image} são serializados como {@code null}, nunca omitidos.
 */
public record UsuarioResponse(UsuarioDados user) {

    public static UsuarioResponse de(UsuarioAutenticado usuario) {
        return new UsuarioResponse(new UsuarioDados(
                usuario.email(), usuario.token(), usuario.username(), usuario.bio(), usuario.image()));
    }

    public record UsuarioDados(String email, String token, String username, String bio, String image) {
    }
}
