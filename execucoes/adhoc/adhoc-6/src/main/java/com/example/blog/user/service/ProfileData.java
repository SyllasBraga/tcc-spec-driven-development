package com.example.blog.user.service;

import com.example.blog.user.persistence.UserAccount;

/**
 * Public profile of a user, as seen by a viewer.
 *
 * @param following whether the viewer follows this user; always {@code false} for anonymous viewers
 */
public record ProfileData(String username, String bio, String image, boolean following) {

    static ProfileData of(UserAccount user, boolean following) {
        return new ProfileData(user.getUsername(), user.getBio(), user.getImage(), following);
    }
}
