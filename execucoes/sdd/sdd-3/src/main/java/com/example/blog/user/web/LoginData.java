package com.example.blog.user.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import com.example.blog.user.service.Credenciais;

public record LoginData(
        @NotBlank(message = "can't be blank") @Email(message = "is invalid") String email,
        @NotBlank(message = "can't be blank") String password) {

    Credenciais paraCredenciais() {
        return new Credenciais(email, password);
    }

    /** Não expõe e-mail nem senha em logs. */
    @Override
    public String toString() {
        return "LoginData[***]";
    }
}
