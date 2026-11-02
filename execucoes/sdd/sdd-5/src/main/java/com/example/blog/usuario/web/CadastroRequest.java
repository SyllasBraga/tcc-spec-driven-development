package com.example.blog.usuario.web;

import com.example.blog.shared.erro.MensagensErro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CadastroRequest(@Valid @NotNull(message = MensagensErro.CORPO_VAZIO) NovoUsuario user) {
}
