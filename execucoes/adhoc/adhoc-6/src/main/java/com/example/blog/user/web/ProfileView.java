package com.example.blog.user.web;

import com.example.blog.user.service.ProfileData;

/**
 * Profile representation of the API contract. {@code bio} and {@code image} are serialized even when null.
 */
public record ProfileView(String username, String bio, String image, boolean following) {

    static ProfileView of(ProfileData profile) {
        return new ProfileView(profile.username(), profile.bio(), profile.image(), profile.following());
    }
}
