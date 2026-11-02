package com.example.blog.shared.web;

/**
 * Conflito com um recurso existente (por exemplo, valor único já usado), convertido em {@code 409}.
 */
public class ConflitoException extends RuntimeException {

    private final String campo;
    private final String mensagem;

    public ConflitoException(String campo, String mensagem) {
        super("Conflito no campo " + campo + ": " + mensagem);
        this.campo = campo;
        this.mensagem = mensagem;
    }

    public String getCampo() {
        return campo;
    }

    public String getMensagem() {
        return mensagem;
    }
}
