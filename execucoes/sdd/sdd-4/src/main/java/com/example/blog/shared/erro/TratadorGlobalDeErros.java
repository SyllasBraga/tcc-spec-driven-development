package com.example.blog.shared.erro;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Converte exceções em respostas no formato de erro do contrato, em um único ponto. */
@RestControllerAdvice
public class TratadorGlobalDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeErros.class);
    private static final String PREFIXO_RAIZ = "user.";
    private static final String MENSAGEM_INVALIDO = "is invalid";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException excecao) {
        Map<String, List<String>> erros = new LinkedHashMap<>();
        for (FieldError erro : excecao.getBindingResult().getFieldErrors()) {
            String campo = removerPrefixoRaiz(erro.getField());
            erros.computeIfAbsent(campo, chave -> new ArrayList<>()).add(erro.getDefaultMessage());
        }
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, new ErroResponse(erros));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> tratarCorpoIlegivel(HttpMessageNotReadableException excecao) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, ErroResponse.de("body", MENSAGEM_INVALIDO));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErroResponse> tratarTipoNaoSuportado(HttpMediaTypeNotSupportedException excecao) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErroResponse.de("body", MENSAGEM_INVALIDO));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErroResponse> tratarMetodoNaoSuportado(HttpRequestMethodNotSupportedException excecao) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, ErroResponse.de("method", "not allowed"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErroResponse> tratarRotaInexistente(NoResourceFoundException excecao) {
        return responder(HttpStatus.NOT_FOUND, ErroResponse.de("path", "not found"));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroResponse> tratarNaoEncontrado(RecursoNaoEncontradoException excecao) {
        return responder(HttpStatus.NOT_FOUND, deErroDeNegocio(excecao));
    }

    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<ErroResponse> tratarConflito(ConflitoException excecao) {
        return responder(HttpStatus.CONFLICT, deErroDeNegocio(excecao));
    }

    @ExceptionHandler(ValidacaoException.class)
    ResponseEntity<ErroResponse> tratarValidacaoDeNegocio(ValidacaoException excecao) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, deErroDeNegocio(excecao));
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    ResponseEntity<ErroResponse> tratarCredenciais(CredenciaisInvalidasException excecao) {
        return responder(HttpStatus.UNAUTHORIZED, ErroResponse.de("credentials", "invalid"));
    }

    @ExceptionHandler(TokenInvalidoException.class)
    ResponseEntity<ErroResponse> tratarTokenInvalido(TokenInvalidoException excecao) {
        return responder(HttpStatus.UNAUTHORIZED, ErroResponse.de("token", MENSAGEM_INVALIDO));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResponse> tratarInesperado(Exception excecao) {
        log.error("Erro inesperado ao processar a requisição", excecao);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, ErroResponse.de("server", "unexpected error"));
    }

    private static ErroResponse deErroDeNegocio(ErroDeNegocioException excecao) {
        return ErroResponse.de(excecao.getChave(), excecao.getMensagem());
    }

    private static String removerPrefixoRaiz(String campo) {
        if (campo.startsWith(PREFIXO_RAIZ)) {
            return campo.substring(PREFIXO_RAIZ.length());
        }
        return campo;
    }

    private static ResponseEntity<ErroResponse> responder(HttpStatus status, ErroResponse corpo) {
        return ResponseEntity.status(status).contentType(ErroResponse.TIPO_DE_CONTEUDO).body(corpo);
    }
}
