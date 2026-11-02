package com.example.blog.user.service;

/**
 * Data for registering a user, already validated for presence and format at the API boundary.
 */
public record NewUser(String username, String email, String password) {

    @Override
    public String toString() {
        return "NewUser[username=" + username + "]";
    }
}
