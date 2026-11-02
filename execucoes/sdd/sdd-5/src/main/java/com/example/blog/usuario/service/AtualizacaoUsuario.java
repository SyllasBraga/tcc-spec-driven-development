package com.example.blog.usuario.service;

import java.util.Objects;

/**
 * Comando de atualização parcial do usuário autenticado, já validado quanto a formato.
 */
public record AtualizacaoUsuario(
        CampoAtualizado<String> email,
        CampoAtualizado<String> username,
        CampoAtualizado<String> senha,
        CampoAtualizado<String> bio,
        CampoAtualizado<String> imagem) {

    public AtualizacaoUsuario {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(senha, "senha");
        Objects.requireNonNull(bio, "bio");
        Objects.requireNonNull(imagem, "imagem");
    }
}
