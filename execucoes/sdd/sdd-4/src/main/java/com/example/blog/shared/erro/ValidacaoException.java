package com.example.blog.shared.erro;

/** Entrada viola uma regra de validação (HTTP 422). */
public class ValidacaoException extends ErroDeNegocioException {

    public ValidacaoException(String chave, String mensagem) {
        super(chave, mensagem);
    }
}
