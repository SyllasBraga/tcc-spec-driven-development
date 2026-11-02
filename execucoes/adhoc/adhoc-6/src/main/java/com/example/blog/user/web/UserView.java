package com.example.blog.user.web;

import com.example.blog.user.service.UserData;

/**
 * User representation of the API contract. {@code bio} and {@code image} are serialized even when null.
 */
public record UserView(String email, String token, String username, String bio, String image) {

    static UserView of(UserData user, String token) {
        return new UserView(user.email(), token, user.username(), user.bio(), user.image());
    }

    @Override
    public String toString() {
        return "UserView[username=" + username + "]";
    }
}
