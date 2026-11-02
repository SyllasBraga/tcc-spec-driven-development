package com.example.blog.shared.error;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Valor que já pertence a outro registro, convertido em {@code 409}.
 */
public class ConflictException extends RuntimeException {

    public static final String MENSAGEM = "has already been taken";

    private final ErrorResponse body;

    /**
     * @param chaves campos em conflito, na ordem em que devem aparecer na resposta
     */
    public ConflictException(List<String> chaves) {
        super("Conflito de unicidade nos campos " + chaves);
        if (chaves.isEmpty()) {
            throw new IllegalArgumentException("ConflictException exige ao menos uma chave");
        }
        Map<String, List<String>> errors = new LinkedHashMap<>();
        chaves.forEach(chave -> errors.put(chave, List.of(MENSAGEM)));
        this.body = ErrorResponse.of(errors);
    }

    public ConflictException(String chave) {
        this(List.of(chave));
    }

    public ErrorResponse body() {
        return body;
    }
}
