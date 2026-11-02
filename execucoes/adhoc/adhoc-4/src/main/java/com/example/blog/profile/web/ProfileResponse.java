package com.example.blog.profile.web;

import com.example.blog.profile.service.ProfileResult;

/** Envelope {@code {"profile": {...}}} das respostas de perfil. */
public record ProfileResponse(ProfileView profile) {

    static ProfileResponse from(ProfileResult result) {
        return new ProfileResponse(ProfileView.from(result));
    }
}
