package com.example.blog.shared.erro;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Corpo padrão de erro do contrato: {@code {"errors": {"<chave>": ["<mensagem>"]}}}.
 */
public record ErrosResponse(Map<String, List<String>> errors) {

	public ErrosResponse {
		Map<String, List<String>> copia = new LinkedHashMap<>();
		errors.forEach((chave, mensagens) -> copia.put(chave, List.copyOf(mensagens)));
		errors = Collections.unmodifiableMap(copia);
	}

	public static ErrosResponse de(String chave, String mensagem) {
		return new ErrosResponse(Map.of(chave, List.of(mensagem)));
	}

}
