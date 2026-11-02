package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code POST /api/users/login}.
 */
public record LoginRequest(@NotNull @Valid LoginFields user) {

    public record LoginFields(
            @NotBlank(message = "can't be blank") String email,
            @NotBlank(message = "can't be blank") String password) {

        @Override
        public String toString() {
            return "LoginFields[email=***, password=***]";
        }
    }
}
