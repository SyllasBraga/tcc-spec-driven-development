package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Corpo de {@code POST /api/users}. */
public record NewUserRequest(@NotNull(message = ValidationMessages.BLANK) @Valid NewUser user) {
}
