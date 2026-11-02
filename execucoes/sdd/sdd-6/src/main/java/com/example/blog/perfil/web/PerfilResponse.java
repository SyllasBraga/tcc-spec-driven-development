package com.example.blog.perfil.web;

import com.example.blog.perfil.service.Perfil;

/**
 * Resposta {@code {"profile": {username, bio, image, following}}}. {@code bio} e {@code image} são sempre
 * serializados, inclusive quando nulos.
 */
public record PerfilResponse(Dados profile) {

	public record Dados(String username, String bio, String image, boolean following) {

	}

	public static PerfilResponse de(Perfil perfil) {
		return new PerfilResponse(new Dados(perfil.username(), perfil.bio(), perfil.image(), perfil.following()));
	}

}
