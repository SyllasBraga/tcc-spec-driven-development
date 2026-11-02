package com.example.blog.profile.web;

import com.example.blog.profile.service.ProfileData;

/** Resposta {@code {"profile": {...}}}. */
public record ProfileResponse(Body profile) {

    public record Body(String username, String bio, String image, boolean following) {
    }

    static ProfileResponse of(ProfileData data) {
        return new ProfileResponse(new Body(data.username(), data.bio(), data.image(), data.following()));
    }
}
