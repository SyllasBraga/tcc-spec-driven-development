package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code POST /api/users/login}: {@code {"user": {...}}}.
 */
public record LoginRequest(@NotNull @Valid LoginData user) {
}
