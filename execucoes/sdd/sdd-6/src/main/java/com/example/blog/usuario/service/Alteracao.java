package com.example.blog.usuario.service;

/**
 * Alteração de um campo em uma atualização parcial: distingue o campo ausente ({@link Manter}) do campo
 * informado, inclusive com valor {@code null} ({@link Definir}).
 *
 * @param <T> tipo do valor do campo
 */
public sealed interface Alteracao<T> {

	/** Campo ausente na requisição: o valor atual é mantido. */
	record Manter<T>() implements Alteracao<T> {

	}

	/** Campo presente na requisição: o valor passa a ser {@code valor}, que pode ser {@code null}. */
	record Definir<T>(T valor) implements Alteracao<T> {

	}

	static <T> Alteracao<T> manter() {
		return new Manter<>();
	}

	static <T> Alteracao<T> definir(T valor) {
		return new Definir<>(valor);
	}

}
