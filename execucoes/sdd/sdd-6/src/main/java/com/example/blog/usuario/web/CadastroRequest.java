package com.example.blog.usuario.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code POST /api/users}. As mesmas restrições valem para {@code PUT /api/user}.
 */
public record CadastroRequest(@NotNull(message = "can't be blank") @Valid Dados user) {

	/** Além do formato aceito por {@code @Email}, exige domínio com ao menos um ponto (recusa {@code a@b}). */
	private static final String EMAIL_COM_DOMINIO = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

	public record Dados(
			@NotBlank(message = "can't be blank") @Size(max = 50, message = "is too long") String username,
			@NotBlank(message = "can't be blank") @Email(regexp = EMAIL_COM_DOMINIO, message = "is invalid")
			@Size(max = 255, message = "is too long") String email,
			@NotBlank(message = "can't be blank") String password) {

	}

}
