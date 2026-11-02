package com.example.blog.shared.erro;

/**
 * Valor de campo único (e-mail, nome de usuário) já usado por outro registro. Vira {@code 409}.
 */
public class CampoJaUtilizadoException extends ErroDeDominioException {

	public static final String MENSAGEM = "has already been taken";

	public CampoJaUtilizadoException(String campo) {
		super(campo, MENSAGEM);
	}

}
