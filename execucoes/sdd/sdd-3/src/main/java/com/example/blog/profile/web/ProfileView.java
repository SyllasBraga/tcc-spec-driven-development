package com.example.blog.profile.web;

import com.example.blog.profile.service.Perfil;

/**
 * Objeto {@code profile} da resposta. {@code bio} e {@code image} saem como {@code null} quando vazios.
 */
public record ProfileView(String username, String bio, String image, boolean following) {

    static ProfileView de(Perfil perfil) {
        return new ProfileView(perfil.username(), perfil.bio(), perfil.image(), perfil.following());
    }
}
