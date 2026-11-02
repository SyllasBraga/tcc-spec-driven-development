package com.example.blog.shared.error;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.blog.shared.web.MediaTypes;

/**
 * Ponto único de conversão de exceções em respostas {@code {"errors":{...}}}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final MediaType JSON_UTF8 = MediaType.parseMediaType(MediaTypes.APPLICATION_JSON_UTF8);
    private static final String CHAVE_CORPO = "body";
    private static final String CORPO_VAZIO = "can't be empty";
    private static final String OBJETO_RAIZ = "user";

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> tratarApi(ApiException ex) {
        return responder(ex.status(), new ErrorResponse(ex.errors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        var erros = new LinkedHashMap<String, List<String>>();
        for (ObjectError erro : ex.getBindingResult().getAllErrors()) {
            String chave = chaveDoErro(erro);
            String mensagem = CHAVE_CORPO.equals(chave) ? CORPO_VAZIO : erro.getDefaultMessage();
            erros.computeIfAbsent(chave, k -> new ArrayList<>()).add(mensagem);
        }
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, new ErrorResponse(erros));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> tratarCorpoIlegivel(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, ErrorResponse.of(CHAVE_CORPO, CORPO_VAZIO));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> tratarTipoDeMidia(HttpMediaTypeNotSupportedException ex) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, ErrorResponse.of(CHAVE_CORPO, "is invalid"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> tratarRotaInexistente(NoResourceFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, ErrorResponse.of("resource", "not found"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> tratarMetodo(HttpRequestMethodNotSupportedException ex) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, ErrorResponse.of("method", "not allowed"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> tratarInesperada(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, ErrorResponse.of("server", "internal error"));
    }

    /** Último segmento do caminho do campo ({@code user.email} vira {@code email}); o objeto raiz vira {@code body}. */
    private static String chaveDoErro(ObjectError erro) {
        if (!(erro instanceof FieldError campo) || OBJETO_RAIZ.equals(campo.getField())) {
            return CHAVE_CORPO;
        }
        String caminho = campo.getField();
        return caminho.substring(caminho.lastIndexOf('.') + 1);
    }

    private static ResponseEntity<ErrorResponse> responder(HttpStatusCode status, ErrorResponse corpo) {
        return ResponseEntity.status(status).contentType(JSON_UTF8).body(corpo);
    }
}
