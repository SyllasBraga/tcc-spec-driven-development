package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Corpo de {@code PUT /api/user}. */
public record UpdateUserRequest(@NotNull(message = ValidationMessages.BLANK) @Valid UpdateUser user) {
}
