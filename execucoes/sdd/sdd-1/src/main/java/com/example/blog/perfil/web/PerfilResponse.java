package com.example.blog.perfil.web;

import com.example.blog.perfil.service.Perfil;

/**
 * Corpo Profile do contrato: {@code {"profile": {"username", "bio", "image", "following"}}}.
 */
public record PerfilResponse(PerfilDados profile) {

    public static PerfilResponse de(Perfil perfil) {
        return new PerfilResponse(
                new PerfilDados(perfil.username(), perfil.bio(), perfil.image(), perfil.isFollowing()));
    }

    public record PerfilDados(String username, String bio, String image, boolean following) {
    }
}
