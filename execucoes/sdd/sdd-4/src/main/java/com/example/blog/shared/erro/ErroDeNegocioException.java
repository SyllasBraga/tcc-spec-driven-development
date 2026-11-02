package com.example.blog.shared.erro;

import java.util.Objects;

/** Base das exceções de domínio que carregam a chave e a mensagem de erro do contrato. */
public abstract class ErroDeNegocioException extends RuntimeException {

    private final String chave;
    private final String mensagem;

    protected ErroDeNegocioException(String chave, String mensagem) {
        super(chave + " " + mensagem);
        this.chave = Objects.requireNonNull(chave, "chave");
        this.mensagem = Objects.requireNonNull(mensagem, "mensagem");
    }

    public String getChave() {
        return chave;
    }

    public String getMensagem() {
        return mensagem;
    }
}
