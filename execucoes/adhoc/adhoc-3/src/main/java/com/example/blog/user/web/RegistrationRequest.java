package com.example.blog.user.web;

import static com.example.blog.shared.error.ValidationMessages.BLANK;
import static com.example.blog.shared.error.ValidationMessages.INVALID;
import static com.example.blog.shared.error.ValidationMessages.TOO_LONG;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.example.blog.user.service.Registration;

/** Corpo de {@code POST /api/users}. */
public record RegistrationRequest(@NotNull(message = BLANK) @Valid NewUser user) {

    public record NewUser(
            @NotBlank(message = BLANK) @Size(max = 64, message = TOO_LONG) String username,
            @NotBlank(message = BLANK) @Email(message = INVALID) @Size(max = 254, message = TOO_LONG) String email,
            @NotBlank(message = BLANK) String password) {
    }

    Registration toRegistration() {
        return new Registration(user.username(), user.email(), user.password());
    }
}
