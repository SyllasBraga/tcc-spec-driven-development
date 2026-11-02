package com.example.blog.shared.web;

/**
 * Recurso inexistente, convertido em {@code 404} com a chave do recurso (por exemplo, {@code profile}).
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    private final String recurso;

    public RecursoNaoEncontradoException(String recurso) {
        super("Recurso não encontrado: " + recurso);
        this.recurso = recurso;
    }

    public String getRecurso() {
        return recurso;
    }
}
