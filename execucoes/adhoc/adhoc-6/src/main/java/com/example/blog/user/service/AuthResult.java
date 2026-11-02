package com.example.blog.user.service;

/**
 * Result of a registration or login: the user and a freshly issued token.
 */
public record AuthResult(UserData user, String token) {

    @Override
    public String toString() {
        return "AuthResult[user=" + user.id() + "]";
    }
}
