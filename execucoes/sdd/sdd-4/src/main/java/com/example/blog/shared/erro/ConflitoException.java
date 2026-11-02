package com.example.blog.shared.erro;

/** Conflito com o estado atual, por exemplo valor único já usado (HTTP 409). */
public class ConflitoException extends ErroDeNegocioException {

    public ConflitoException(String chave, String mensagem) {
        super(chave, mensagem);
    }
}
