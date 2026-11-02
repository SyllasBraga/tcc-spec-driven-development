package com.example.blog.profile.web;

import com.example.blog.profile.service.Profile;

/**
 * Resposta Profile do contrato. {@code bio} e {@code image} são sempre serializados, mesmo nulos.
 */
public record ProfileResponse(ProfileBody profile) {

    public record ProfileBody(String username, String bio, String image, boolean following) {
    }

    public static ProfileResponse from(Profile perfil) {
        return new ProfileResponse(
                new ProfileBody(perfil.username(), perfil.bio(), perfil.image(), perfil.following()));
    }
}
