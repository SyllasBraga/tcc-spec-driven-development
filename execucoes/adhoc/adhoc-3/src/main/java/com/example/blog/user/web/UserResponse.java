package com.example.blog.user.web;

import com.example.blog.user.service.UserData;

/** Resposta do tipo User do contrato: {@code {"user": {...}}}. */
public record UserResponse(UserBody user) {

    public record UserBody(String email, String token, String username, String bio, String image) {
    }

    static UserResponse of(UserData data, String token) {
        return new UserResponse(new UserBody(data.email(), token, data.username(), data.bio(), data.image()));
    }
}
