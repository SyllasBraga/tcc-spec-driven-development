package com.example.blog.user.service;

import com.example.blog.user.persistence.UserAccount;

/**
 * Public data of a user, without credentials.
 */
public record UserData(long id, String email, String username, String bio, String image) {

    static UserData from(UserAccount user) {
        return new UserData(user.getId(), user.getEmail(), user.getUsername(), user.getBio(), user.getImage());
    }
}
