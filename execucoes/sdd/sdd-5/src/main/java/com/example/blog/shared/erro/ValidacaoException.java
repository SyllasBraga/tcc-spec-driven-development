package com.example.blog.shared.erro;

import java.util.List;
import java.util.Map;

/**
 * Erro de validação de regra (422), com as mensagens agrupadas por campo.
 */
public class ValidacaoException extends RuntimeException {

    private final Map<String, List<String>> erros;

    public ValidacaoException(Map<String, List<String>> erros) {
        super("Erro de validação nos campos " + erros.keySet());
        this.erros = ErrosResponse.copiarPreservandoOrdem(erros);
    }

    public static ValidacaoException de(String chave, String mensagem) {
        return new ValidacaoException(Map.of(chave, List.of(mensagem)));
    }

    public Map<String, List<String>> getErros() {
        return erros;
    }
}
