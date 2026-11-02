package com.example.blog.usuario.web;

import static com.example.blog.shared.erro.MensagensDeErro.INVALIDO;
import static com.example.blog.shared.erro.MensagensDeErro.NAO_PODE_FICAR_EM_BRANCO;

import jakarta.validation.GroupSequence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corpo de {@code POST /api/users}. */
public record CadastroRequest(@Valid @NotNull(message = NAO_PODE_FICAR_EM_BRANCO) Dados user) {

    @GroupSequence({Dados.class, ValidacaoDeFormato.class})
    public record Dados(
            @NotBlank(message = NAO_PODE_FICAR_EM_BRANCO) String username,
            @NotBlank(message = NAO_PODE_FICAR_EM_BRANCO)
            @Email(message = INVALIDO, groups = ValidacaoDeFormato.class) String email,
            @NotBlank(message = NAO_PODE_FICAR_EM_BRANCO) String password) {
    }
}
