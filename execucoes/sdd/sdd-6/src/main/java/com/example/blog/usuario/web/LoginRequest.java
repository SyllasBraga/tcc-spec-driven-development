package com.example.blog.usuario.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code POST /api/users/login}.
 */
public record LoginRequest(@NotNull(message = "can't be blank") @Valid Dados user) {

	public record Dados(@NotBlank(message = "can't be blank") String email,
			@NotBlank(message = "can't be blank") String password) {

	}

}
