package com.example.blog.profile.web;

import com.example.blog.profile.service.ProfileData;

/** Resposta do tipo Profile do contrato: {@code {"profile": {...}}}. */
public record ProfileResponse(ProfileBody profile) {

    public record ProfileBody(String username, String bio, String image, boolean following) {
    }

    static ProfileResponse from(ProfileData data) {
        return new ProfileResponse(new ProfileBody(data.username(), data.bio(), data.image(), data.following()));
    }
}
