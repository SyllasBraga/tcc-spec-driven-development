package com.example.blog.profile.web;

import com.example.blog.profile.service.ProfileResult;

/** Perfil na resposta (schema {@code Profile}); {@code bio} e {@code image} podem ser {@code null}. */
public record ProfileView(String username, String bio, String image, boolean following) {

    static ProfileView from(ProfileResult result) {
        return new ProfileView(result.username(), result.bio(), result.image(), result.following());
    }
}
