package com.example.blog.usuario.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code POST /api/users/login}: {@code {"user": {"email", "password"}}}.
 */
public record LoginRequest(@Valid @NotNull(message = "is invalid") Credenciais user) {

    public record Credenciais(
            @NotBlank(message = "can't be blank") String email,
            @NotBlank(message = "can't be blank") String password) {
    }
}
