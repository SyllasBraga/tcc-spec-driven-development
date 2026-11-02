package com.example.blog.shared.erro;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Corpo de erro do contrato ({@code GenericErrorModel}): {@code {"errors":{"<chave>":["<mensagem>"]}}}.
 */
public record ErrosResponse(Map<String, List<String>> errors) {

    public ErrosResponse {
        errors = copiarPreservandoOrdem(errors);
    }

    public static ErrosResponse de(String chave, String mensagem) {
        return new ErrosResponse(Map.of(chave, List.of(mensagem)));
    }

    static Map<String, List<String>> copiarPreservandoOrdem(Map<String, List<String>> origem) {
        Map<String, List<String>> copia = new LinkedHashMap<>();
        origem.forEach((chave, mensagens) -> copia.put(chave, List.copyOf(mensagens)));
        return Collections.unmodifiableMap(copia);
    }
}
