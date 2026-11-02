package com.example.blog.user.web;

import com.example.blog.user.service.UserResult;

/** Envelope {@code {"user": {...}}} das respostas de usuário. */
public record UserResponse(UserView user) {

    static UserResponse from(UserResult result) {
        return new UserResponse(UserView.from(result));
    }
}
