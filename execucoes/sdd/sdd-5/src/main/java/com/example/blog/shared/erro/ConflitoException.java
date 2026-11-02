package com.example.blog.shared.erro;

import java.util.List;
import java.util.Map;

/**
 * Conflito com dados existentes (409), com as mensagens agrupadas por campo.
 */
public class ConflitoException extends RuntimeException {

    private final Map<String, List<String>> erros;

    public ConflitoException(Map<String, List<String>> erros) {
        super("Conflito nos campos " + erros.keySet());
        this.erros = ErrosResponse.copiarPreservandoOrdem(erros);
    }

    public static ConflitoException de(String chave, String mensagem) {
        return new ConflitoException(Map.of(chave, List.of(mensagem)));
    }

    public Map<String, List<String>> getErros() {
        return erros;
    }
}
