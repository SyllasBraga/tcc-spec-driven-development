package com.example.blog.user.web;

import com.example.blog.user.service.UserData;

/** Resposta {@code {"user": {...}}} com o token do usuário autenticado. */
public record UserResponse(Body user) {

    public record Body(String email, String token, String username, String bio, String image) {
    }

    static UserResponse of(UserData data, String token) {
        return new UserResponse(new Body(data.email(), token, data.username(), data.bio(), data.image()));
    }
}
