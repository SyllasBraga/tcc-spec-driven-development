package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Corpo de {@code POST /api/users/login}. */
public record LoginUserRequest(@NotNull(message = ValidationMessages.BLANK) @Valid LoginUser user) {
}
