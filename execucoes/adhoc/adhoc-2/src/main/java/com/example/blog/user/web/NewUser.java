package com.example.blog.user.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.example.blog.shared.error.ValidationMessages;
import com.example.blog.user.service.Registration;

/** Dados de cadastro recebidos na API. */
public record NewUser(
        @NotBlank(message = ValidationMessages.BLANK)
        @Size(max = UserFieldLimits.USERNAME, message = ValidationMessages.TOO_LONG)
        String username,
        @NotBlank(message = ValidationMessages.BLANK)
        @Email(message = ValidationMessages.INVALID)
        @Size(max = UserFieldLimits.EMAIL, message = ValidationMessages.TOO_LONG)
        String email,
        @NotBlank(message = ValidationMessages.BLANK)
        @Size(max = UserFieldLimits.PASSWORD, message = ValidationMessages.TOO_LONG)
        String password) {

    Registration toRegistration() {
        return new Registration(username, email, password);
    }
}
