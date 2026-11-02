package com.example.blog.user.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.example.blog.shared.error.ValidationMessages;

/**
 * Fields of {@code POST /api/users}.
 */
public record NewUserPayload(
        @NotBlank(message = ValidationMessages.BLANK)
        @Size(max = 100, message = ValidationMessages.TOO_LONG + " (maximum is {max} characters)")
        String username,

        @NotBlank(message = ValidationMessages.BLANK)
        @Email(message = ValidationMessages.INVALID)
        @Size(max = 255, message = ValidationMessages.TOO_LONG + " (maximum is {max} characters)")
        String email,

        @NotBlank(message = ValidationMessages.BLANK)
        String password) {

    @Override
    public String toString() {
        return "NewUserPayload[username=" + username + "]";
    }
}
