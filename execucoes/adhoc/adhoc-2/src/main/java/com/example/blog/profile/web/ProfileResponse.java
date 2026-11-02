package com.example.blog.profile.web;

import com.example.blog.profile.service.ProfileView;

/** Envelope {@code {"profile": {...}}} das respostas de perfil. */
public record ProfileResponse(ProfileBody profile) {

    static ProfileResponse from(ProfileView view) {
        return new ProfileResponse(new ProfileBody(view.username(), view.bio(), view.image(), view.following()));
    }
}
