package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code POST /api/users}: {@code {"user": {...}}}.
 */
public record NewUserRequest(@NotNull @Valid NewUserData user) {
}
