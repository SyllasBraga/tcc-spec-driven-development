package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.example.blog.shared.error.ValidationMessages;

/** Corpo de {@code POST /api/users/login}. */
public record LoginRequest(@Valid @NotNull(message = ValidationMessages.BLANK) LoginUser user) {
}
