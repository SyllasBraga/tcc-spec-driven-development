package com.example.blog.user.web;

/** Envelope {@code {"user": {...}}} das respostas de usuário. */
public record UserResponse(UserBody user) {
}
