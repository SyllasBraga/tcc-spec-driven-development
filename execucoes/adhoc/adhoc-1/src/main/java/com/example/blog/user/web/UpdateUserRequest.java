package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.example.blog.shared.error.ErrorMessages;

public record UpdateUserRequest(@NotNull(message = ErrorMessages.BLANK) @Valid UpdateUser user) {
}
