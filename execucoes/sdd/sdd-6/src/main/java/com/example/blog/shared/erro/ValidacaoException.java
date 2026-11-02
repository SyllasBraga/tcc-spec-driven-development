package com.example.blog.shared.erro;

/**
 * Violação de regra de validação de entrada. Vira {@code 422}.
 */
public class ValidacaoException extends ErroDeDominioException {

	public ValidacaoException(String campo, String mensagem) {
		super(campo, mensagem);
	}

}
