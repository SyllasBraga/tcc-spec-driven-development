package com.example.blog.shared.erro;

import java.util.Objects;

/**
 * Base das exceções de domínio que viram respostas {@code {"errors": {"<chave>": ["<mensagem>"]}}}.
 */
public abstract class ErroDeDominioException extends RuntimeException {

	private final String chave;

	private final String mensagem;

	protected ErroDeDominioException(String chave, String mensagem) {
		super(chave + ": " + mensagem);
		this.chave = Objects.requireNonNull(chave, "chave");
		this.mensagem = Objects.requireNonNull(mensagem, "mensagem");
	}

	public String getChave() {
		return chave;
	}

	public String getMensagem() {
		return mensagem;
	}

	public ErrosResponse paraResposta() {
		return ErrosResponse.de(chave, mensagem);
	}

}
