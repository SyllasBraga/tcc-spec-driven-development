package com.example.blog.shared.web;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Erro de validação de regra de negócio ou de entrada, convertido em {@code 422}.
 */
public class ValidacaoException extends RuntimeException {

    private final Map<String, List<String>> erros;

    public ValidacaoException(Map<String, List<String>> erros) {
        super("Erro de validação nos campos " + erros.keySet());
        Map<String, List<String>> copia = new LinkedHashMap<>();
        erros.forEach((campo, mensagens) -> copia.put(campo, List.copyOf(mensagens)));
        this.erros = Collections.unmodifiableMap(copia);
    }

    public ValidacaoException(String campo, String mensagem) {
        this(Map.of(campo, List.of(mensagem)));
    }

    public Map<String, List<String>> getErros() {
        return erros;
    }
}
