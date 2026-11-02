package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.example.blog.shared.error.ValidationMessages;

/** Corpo de {@code PUT /api/user}. */
public record UpdateUserRequest(@Valid @NotNull(message = ValidationMessages.BLANK) UpdateUser user) {
}
