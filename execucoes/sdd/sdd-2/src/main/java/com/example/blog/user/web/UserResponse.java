package com.example.blog.user.web;

import com.example.blog.user.service.AuthenticatedUser;

/**
 * Resposta User do contrato. {@code bio} e {@code image} são sempre serializados, mesmo nulos.
 */
public record UserResponse(UserBody user) {

    public record UserBody(String email, String token, String username, String bio, String image) {
    }

    public static UserResponse from(AuthenticatedUser usuario) {
        return new UserResponse(new UserBody(
                usuario.email(), usuario.token(), usuario.username(), usuario.bio(), usuario.image()));
    }
}
