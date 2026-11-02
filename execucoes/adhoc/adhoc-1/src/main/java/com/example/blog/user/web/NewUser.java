package com.example.blog.user.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.example.blog.shared.error.ErrorMessages;
import com.example.blog.user.service.Registration;

public record NewUser(
        @NotBlank(message = ErrorMessages.BLANK)
        @Size(max = UserLimits.USERNAME_MAX, message = ErrorMessages.TOO_LONG)
        String username,

        @NotBlank(message = ErrorMessages.BLANK)
        @Email(message = ErrorMessages.INVALID)
        @Size(max = UserLimits.EMAIL_MAX, message = ErrorMessages.TOO_LONG)
        String email,

        @NotBlank(message = ErrorMessages.BLANK)
        @Size(max = UserLimits.PASSWORD_MAX, message = ErrorMessages.TOO_LONG)
        String password) {

    Registration toRegistration() {
        return new Registration(username, email, password);
    }
}
