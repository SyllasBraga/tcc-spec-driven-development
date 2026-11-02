package com.example.blog.shared.erro;

/** Recurso solicitado não existe (HTTP 404). */
public class RecursoNaoEncontradoException extends ErroDeNegocioException {

    public RecursoNaoEncontradoException(String chave, String mensagem) {
        super(chave, mensagem);
    }
}
