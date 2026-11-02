package com.example.blog.user.web;

import jakarta.validation.constraints.NotBlank;

import com.example.blog.shared.error.ErrorMessages;

public record LoginUser(
        @NotBlank(message = ErrorMessages.BLANK) String email,
        @NotBlank(message = ErrorMessages.BLANK) String password) {
}
