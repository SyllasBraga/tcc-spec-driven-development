package com.example.blog.user.web;

import static com.example.blog.shared.error.ValidationMessages.BLANK;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corpo de {@code POST /api/users/login}. */
public record LoginRequest(@NotNull(message = BLANK) @Valid Credentials user) {

    public record Credentials(
            @NotBlank(message = BLANK) String email,
            @NotBlank(message = BLANK) String password) {
    }
}
