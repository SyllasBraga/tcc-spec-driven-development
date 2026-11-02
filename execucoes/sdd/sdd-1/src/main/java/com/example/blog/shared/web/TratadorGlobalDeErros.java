package com.example.blog.shared.web;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.blog.shared.security.TokenInvalidoException;

/**
 * Ponto único de conversão de exceções no corpo de erro do contrato ({@code {"errors": {...}}}).
 * Nunca expõe stack trace nem detalhes internos ao cliente.
 */
@RestControllerAdvice
public class TratadorGlobalDeErros {

    static final String CHAVE_CORPO = "body";
    static final String CAMPO_RAIZ = "user";
    static final String MENSAGEM_INVALIDO = "is invalid";

    /** Charset explícito: no fluxo de exceção o filtro de codificação não garante o parâmetro (FR-028). */
    private static final MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeErros.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, List<String>> erros = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            List<String> mensagens = erros.computeIfAbsent(chaveDoCampo(erro.getField()), chave -> new ArrayList<>());
            String mensagem = erro.getDefaultMessage() == null ? MENSAGEM_INVALIDO : erro.getDefaultMessage();
            if (!mensagens.contains(mensagem)) {
                mensagens.add(mensagem);
            }
        }
        if (erros.isEmpty()) {
            erros.put(CHAVE_CORPO, List.of(MENSAGEM_INVALIDO));
        }
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, new ErroResponse(erros));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoIlegivel(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, ErroResponse.de(CHAVE_CORPO, MENSAGEM_INVALIDO));
    }

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<ErroResponse> tratarValidacaoDeDominio(ValidacaoException ex) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, new ErroResponse(ex.getErros()));
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> tratarConflito(ConflitoException ex) {
        return responder(HttpStatus.CONFLICT, ErroResponse.de(ex.getCampo(), ex.getMensagem()));
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> tratarCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        return responder(HttpStatus.UNAUTHORIZED, ErroResponse.de("credentials", "invalid"));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ErroResponse.de(ex.getRecurso(), "not found"));
    }

    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ErroResponse> tratarTokenInvalido(TokenInvalidoException ex) {
        return responder(HttpStatus.UNAUTHORIZED, ErroResponse.de("token", MENSAGEM_INVALIDO));
    }

    /**
     * Fronteira final: exceções do Spring que já carregam status (rota inexistente, método não suportado)
     * mantêm o status; as demais viram {@code 500} com mensagem genérica.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarInesperado(Exception ex) {
        if (ex instanceof ErrorResponse erroDoSpring) {
            HttpStatusCode status = erroDoSpring.getStatusCode();
            HttpStatus statusConhecido = HttpStatus.resolve(status.value());
            String mensagem = statusConhecido == null
                    ? MENSAGEM_INVALIDO
                    : statusConhecido.getReasonPhrase().toLowerCase(Locale.ROOT);
            return responder(status, ErroResponse.de("request", mensagem));
        }
        log.error("Erro inesperado ao processar a requisição", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, ErroResponse.de("server", "unexpected error"));
    }

    private static String chaveDoCampo(String caminho) {
        if (CAMPO_RAIZ.equals(caminho)) {
            return CHAVE_CORPO;
        }
        int ultimoPonto = caminho.lastIndexOf('.');
        return ultimoPonto < 0 ? caminho : caminho.substring(ultimoPonto + 1);
    }

    private static ResponseEntity<ErroResponse> responder(HttpStatusCode status, ErroResponse corpo) {
        return ResponseEntity.status(status).contentType(JSON_UTF8).body(corpo);
    }
}
