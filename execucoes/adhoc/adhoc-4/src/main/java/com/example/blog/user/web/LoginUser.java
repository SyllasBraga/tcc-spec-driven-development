package com.example.blog.user.web;

import jakarta.validation.constraints.NotBlank;

/** Credenciais de login (schema {@code LoginUser}). */
public record LoginUser(
        @NotBlank(message = ValidationMessages.BLANK) String email,
        @NotBlank(message = ValidationMessages.BLANK) String password) {
}
