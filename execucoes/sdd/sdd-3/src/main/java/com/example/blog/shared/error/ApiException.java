package com.example.blog.shared.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Base das exceções de domínio que viram resposta HTTP de erro.
 *
 * <p>As exceções de cada funcionalidade estendem esta classe, informando o status e o mapa de erros. Assim o
 * {@link GlobalExceptionHandler} trata uma única classe e o pacote {@code shared} não depende das funcionalidades.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, List<String>> errors;

    /**
     * @param status status HTTP da resposta
     * @param errors mensagens por chave, enviadas ao cliente
     * @param mensagemDeLog mensagem interna da exceção, sem dados sensíveis
     */
    protected ApiException(HttpStatus status, Map<String, List<String>> errors, String mensagemDeLog) {
        super(mensagemDeLog);
        this.status = status;
        var copia = new LinkedHashMap<String, List<String>>();
        errors.forEach((chave, mensagens) -> copia.put(chave, List.copyOf(mensagens)));
        this.errors = Collections.unmodifiableMap(copia);
    }

    public HttpStatus status() {
        return status;
    }

    public Map<String, List<String>> errors() {
        return errors;
    }
}
