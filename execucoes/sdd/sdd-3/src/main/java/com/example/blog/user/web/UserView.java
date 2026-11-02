package com.example.blog.user.web;

import com.example.blog.user.service.UsuarioAutenticado;

/**
 * Objeto {@code user} da resposta. {@code bio} e {@code image} saem como {@code null} quando vazios.
 */
public record UserView(String email, String token, String username, String bio, String image) {

    static UserView de(UsuarioAutenticado usuario) {
        return new UserView(usuario.email(), usuario.token(), usuario.username(), usuario.bio(), usuario.image());
    }
}
