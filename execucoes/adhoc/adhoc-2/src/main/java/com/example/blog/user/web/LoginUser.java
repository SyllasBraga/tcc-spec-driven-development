package com.example.blog.user.web;

import jakarta.validation.constraints.NotBlank;

import com.example.blog.shared.error.ValidationMessages;
import com.example.blog.user.service.Credentials;

/** Credenciais recebidas na API. */
public record LoginUser(
        @NotBlank(message = ValidationMessages.BLANK) String email,
        @NotBlank(message = ValidationMessages.BLANK) String password) {

    Credentials toCredentials() {
        return new Credentials(email, password);
    }
}
