package com.example.blog.shared.error;

import java.util.List;
import java.util.Map;

/**
 * Corpo de erro do contrato Conduit: {@code {"errors": {"campo": ["mensagem"]}}}.
 *
 * @param errors mensagens agrupadas pela chave do campo ou do recurso
 */
public record ApiErrors(Map<String, List<String>> errors) {

    public ApiErrors {
        errors = Map.copyOf(errors);
    }

    public static ApiErrors of(String key, String message) {
        return new ApiErrors(Map.of(key, List.of(message)));
    }
}
