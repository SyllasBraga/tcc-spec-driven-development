package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corpo de {@code POST /api/users/login}. */
public record LoginRequest(@NotNull(message = ValidationMessages.BLANK) @Valid Credentials user) {

    public record Credentials(
            @NotBlank(message = ValidationMessages.BLANK) String email,
            @NotBlank(message = ValidationMessages.BLANK) String password) {
    }
}
