package com.example.blog.usuario.web;

import static com.example.blog.shared.erro.MensagensDeErro.NAO_PODE_FICAR_EM_BRANCO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corpo de {@code POST /api/users/login}. */
public record LoginRequest(@Valid @NotNull(message = NAO_PODE_FICAR_EM_BRANCO) Dados user) {

    public record Dados(
            @NotBlank(message = NAO_PODE_FICAR_EM_BRANCO) String email,
            @NotBlank(message = NAO_PODE_FICAR_EM_BRANCO) String password) {
    }
}
