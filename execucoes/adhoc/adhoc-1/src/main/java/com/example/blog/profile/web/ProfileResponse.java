package com.example.blog.profile.web;

import com.example.blog.profile.service.ProfileData;

public record ProfileResponse(ProfileView profile) {

    static ProfileResponse of(ProfileData profile) {
        return new ProfileResponse(ProfileView.of(profile));
    }
}
