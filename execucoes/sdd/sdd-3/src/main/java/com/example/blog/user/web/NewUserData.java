package com.example.blog.user.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import com.example.blog.user.service.NovoUsuario;

public record NewUserData(
        @NotBlank(message = "can't be blank") String username,
        @NotBlank(message = "can't be blank") @Email(message = "is invalid") String email,
        @NotBlank(message = "can't be blank") String password) {

    NovoUsuario paraNovoUsuario() {
        return new NovoUsuario(username, email, password);
    }

    /** Não expõe a senha em logs. */
    @Override
    public String toString() {
        return "NewUserData[username=" + username + "]";
    }
}
