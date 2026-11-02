package com.example.blog.shared.erro;

/**
 * Recurso inexistente. Vira {@code 404} com a chave do recurso e a mensagem {@code "not found"}.
 */
public class RecursoNaoEncontradoException extends ErroDeDominioException {

	public static final String MENSAGEM = "not found";

	public RecursoNaoEncontradoException(String recurso) {
		super(recurso, MENSAGEM);
	}

}
