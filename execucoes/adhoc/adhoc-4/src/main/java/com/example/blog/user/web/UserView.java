package com.example.blog.user.web;

import com.example.blog.user.service.UserResult;

/** Usuário autenticado na resposta (schema {@code User}); {@code bio} e {@code image} podem ser {@code null}. */
public record UserView(String email, String token, String username, String bio, String image) {

    static UserView from(UserResult result) {
        return new UserView(result.email(), result.token(), result.username(), result.bio(), result.image());
    }
}
