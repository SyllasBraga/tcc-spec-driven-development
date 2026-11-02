package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.example.blog.shared.error.ValidationMessages;

public record LoginRequest(@NotNull(message = ValidationMessages.BLANK) @Valid LoginPayload user) {
}
