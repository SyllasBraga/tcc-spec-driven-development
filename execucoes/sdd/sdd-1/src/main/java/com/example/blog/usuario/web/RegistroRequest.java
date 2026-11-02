package com.example.blog.usuario.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code POST /api/users}: {@code {"user": {"username", "email", "password"}}}.
 */
public record RegistroRequest(@Valid @NotNull(message = "is invalid") UsuarioNovo user) {

    public record UsuarioNovo(
            @NotBlank(message = "can't be blank") String username,
            @NotBlank(message = "can't be blank") @Email(message = "is invalid") String email,
            @NotBlank(message = "can't be blank") String password) {
    }
}
