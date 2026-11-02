package com.example.blog.usuario.web;

import com.example.blog.shared.erro.MensagensErro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NovoUsuario(
        @NotBlank(message = MensagensErro.NAO_PODE_SER_VAZIO) String username,
        @NotBlank(message = MensagensErro.NAO_PODE_SER_VAZIO) @Email(message = MensagensErro.INVALIDO) String email,
        @NotBlank(message = MensagensErro.NAO_PODE_SER_VAZIO) String password) {
}
