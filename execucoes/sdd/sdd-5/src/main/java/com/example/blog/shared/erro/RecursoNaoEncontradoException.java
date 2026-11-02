package com.example.blog.shared.erro;

/**
 * Recurso inexistente (404). A chave identifica o recurso no corpo de erro, por exemplo {@code profile}.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    private final String chave;

    public RecursoNaoEncontradoException(String chave) {
        super("Recurso não encontrado: " + chave);
        this.chave = chave;
    }

    public String getChave() {
        return chave;
    }
}
