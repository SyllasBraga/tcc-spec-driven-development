package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code POST /api/users}.
 */
public record RegisterUserRequest(@NotNull @Valid NewUserFields user) {

    /**
     * Espaços nas pontas de {@code username} e {@code email} são removidos antes da validação.
     */
    public record NewUserFields(
            @NotBlank(message = "can't be blank") String username,
            @NotBlank(message = "can't be blank") @Email(message = "is invalid") String email,
            @NotBlank(message = "can't be blank") String password) {

        public NewUserFields {
            username = username == null ? null : username.trim();
            email = email == null ? null : email.trim();
        }

        @Override
        public String toString() {
            return "NewUserFields[username=" + username + ", email=***, password=***]";
        }
    }
}
