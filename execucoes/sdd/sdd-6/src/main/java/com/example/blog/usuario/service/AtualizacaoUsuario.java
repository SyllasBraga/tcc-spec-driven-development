package com.example.blog.usuario.service;

import java.util.Objects;

/**
 * Atualização parcial do usuário atual. Cada campo indica se deve ser mantido ou redefinido.
 */
public record AtualizacaoUsuario(Alteracao<String> email, Alteracao<String> username, Alteracao<String> senha,
		Alteracao<String> bio, Alteracao<String> image) {

	public AtualizacaoUsuario {
		Objects.requireNonNull(email, "email");
		Objects.requireNonNull(username, "username");
		Objects.requireNonNull(senha, "senha");
		Objects.requireNonNull(bio, "bio");
		Objects.requireNonNull(image, "image");
	}

}
