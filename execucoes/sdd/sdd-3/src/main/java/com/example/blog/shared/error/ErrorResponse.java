package com.example.blog.shared.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Corpo de erro padrão da API: {@code {"errors":{"<chave>":["<mensagem>"]}}}.
 *
 * @param errors mensagens por chave, na ordem de inserção
 */
public record ErrorResponse(Map<String, List<String>> errors) {

    public ErrorResponse {
        var copia = new LinkedHashMap<String, List<String>>();
        errors.forEach((chave, mensagens) -> copia.put(chave, List.copyOf(mensagens)));
        errors = Collections.unmodifiableMap(copia);
    }

    public static ErrorResponse of(String chave, String mensagem) {
        return new ErrorResponse(Map.of(chave, List.of(mensagem)));
    }
}
