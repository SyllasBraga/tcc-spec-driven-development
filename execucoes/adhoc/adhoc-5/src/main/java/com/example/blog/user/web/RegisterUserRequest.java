package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /api/users}. */
public record RegisterUserRequest(@NotNull(message = ValidationMessages.BLANK) @Valid NewUser user) {

    public record NewUser(
            @NotBlank(message = ValidationMessages.BLANK)
            @Size(max = 50, message = ValidationMessages.USERNAME_TOO_LONG)
            @Pattern(regexp = ValidationMessages.USERNAME_PATTERN, message = ValidationMessages.INVALID)
            String username,

            @NotBlank(message = ValidationMessages.BLANK)
            @Size(max = 254, message = ValidationMessages.EMAIL_TOO_LONG)
            @Email(regexp = ValidationMessages.EMAIL_DOMAIN_PATTERN, message = ValidationMessages.INVALID)
            String email,

            @NotBlank(message = ValidationMessages.BLANK)
            String password) {
    }
}
