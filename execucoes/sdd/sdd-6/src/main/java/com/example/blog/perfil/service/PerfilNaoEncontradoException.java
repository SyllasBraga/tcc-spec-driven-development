package com.example.blog.perfil.service;

import com.example.blog.shared.erro.RecursoNaoEncontradoException;

/**
 * Nenhum usuário com o nome informado. Vira {@code 404} com {@code profile: ["not found"]}.
 */
public class PerfilNaoEncontradoException extends RecursoNaoEncontradoException {

	public PerfilNaoEncontradoException() {
		super("profile");
	}

}
