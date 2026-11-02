package com.example.blog.shared.web;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Corpo de erro do contrato ({@code GenericErrorModel}): {@code {"errors": {"<chave>": ["<mensagem>"]}}}.
 *
 * @param errors mensagens agrupadas por chave
 */
public record ErroResponse(Map<String, List<String>> errors) {

    public ErroResponse {
        Map<String, List<String>> copia = new LinkedHashMap<>();
        errors.forEach((chave, mensagens) -> copia.put(chave, List.copyOf(mensagens)));
        errors = Collections.unmodifiableMap(copia);
    }

    public static ErroResponse de(String chave, String mensagem) {
        return new ErroResponse(Map.of(chave, List.of(mensagem)));
    }
}
