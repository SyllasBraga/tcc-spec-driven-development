package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code PUT /api/user}: {@code {"user": {...}}}.
 */
public record UpdateUserRequest(@NotNull @Valid UpdateUserData user) {
}
