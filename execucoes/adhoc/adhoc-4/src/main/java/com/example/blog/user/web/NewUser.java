package com.example.blog.user.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados de cadastro (schema {@code NewUser}). */
public record NewUser(
        @NotBlank(message = ValidationMessages.BLANK)
        @Size(max = 100, message = ValidationMessages.TOO_LONG)
        String username,

        @NotBlank(message = ValidationMessages.BLANK)
        @Email(message = ValidationMessages.INVALID)
        @Size(max = 254, message = ValidationMessages.TOO_LONG)
        String email,

        @NotBlank(message = ValidationMessages.BLANK)
        String password) {
}
