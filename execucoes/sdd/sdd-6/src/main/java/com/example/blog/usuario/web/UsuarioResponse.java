package com.example.blog.usuario.web;

import com.example.blog.usuario.service.UsuarioAutenticado;

/**
 * Resposta {@code {"user": {email, token, username, bio, image}}}. {@code bio} e {@code image} são sempre
 * serializados, inclusive quando nulos.
 */
public record UsuarioResponse(Dados user) {

	public record Dados(String email, String token, String username, String bio, String image) {

	}

	public static UsuarioResponse de(UsuarioAutenticado usuario) {
		return new UsuarioResponse(
				new Dados(usuario.email(), usuario.token(), usuario.username(), usuario.bio(), usuario.image()));
	}

}
