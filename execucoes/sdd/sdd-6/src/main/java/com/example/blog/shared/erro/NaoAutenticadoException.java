package com.example.blog.shared.erro;

/**
 * Falha de autenticação detectada pela aplicação. Vira {@code 401}.
 */
public class NaoAutenticadoException extends ErroDeDominioException {

	public NaoAutenticadoException(String chave, String mensagem) {
		super(chave, mensagem);
	}

	/** Credenciais de login inválidas: {@code credentials: ["invalid"]}. */
	public static NaoAutenticadoException credenciaisInvalidas() {
		return new NaoAutenticadoException("credentials", "invalid");
	}

	/** Token válido de um usuário que não existe mais: {@code token: ["is invalid"]}. */
	public static NaoAutenticadoException tokenInvalido() {
		return new NaoAutenticadoException("token", "is invalid");
	}

}
