package com.example.blog.profile.web;

import com.example.blog.profile.service.ProfileData;

public record ProfileView(String username, String bio, String image, boolean following) {

    static ProfileView of(ProfileData profile) {
        return new ProfileView(profile.username(), profile.bio(), profile.image(), profile.following());
    }
}
