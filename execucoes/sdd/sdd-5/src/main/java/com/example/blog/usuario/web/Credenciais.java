package com.example.blog.usuario.web;

import com.example.blog.shared.erro.MensagensErro;
import jakarta.validation.constraints.NotBlank;

public record Credenciais(
        @NotBlank(message = MensagensErro.NAO_PODE_SER_VAZIO) String email,
        @NotBlank(message = MensagensErro.NAO_PODE_SER_VAZIO) String password) {
}
