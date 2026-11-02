package com.example.blog.user.web;

import jakarta.validation.constraints.NotBlank;

import com.example.blog.shared.error.ValidationMessages;

/**
 * Fields of {@code POST /api/users/login}.
 */
public record LoginPayload(
        @NotBlank(message = ValidationMessages.BLANK) String email,
        @NotBlank(message = ValidationMessages.BLANK) String password) {

    @Override
    public String toString() {
        return "LoginPayload[]";
    }
}
