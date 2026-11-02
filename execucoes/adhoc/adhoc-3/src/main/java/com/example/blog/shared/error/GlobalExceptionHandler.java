package com.example.blog.shared.error;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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

/**
 * Ponto único de conversão de exceções em respostas no formato de erro do contrato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return respond(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(UnprocessableException.class)
    ResponseEntity<ErrorResponse> handleUnprocessable(UnprocessableException ex) {
        return respond(HttpStatus.UNPROCESSABLE_CONTENT, ex);
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return respond(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(lastSegment(fieldError.getField()), key -> new ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }
        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            errors.computeIfAbsent(globalError.getObjectName(), key -> new ArrayList<>())
                    .add(globalError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(new ErrorResponse(errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("Corpo da requisição ilegível", ex);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(ErrorResponse.of("body", "is invalid"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of("body", "must be application/json"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ErrorResponse.of("method", "is not allowed"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of("resource", "not found"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Corrida entre a checagem de unicidade e o INSERT/UPDATE: a restrição do banco decide.
        log.warn("Violação de integridade tratada como conflito", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of("resource", "has already been taken"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse springError
                && springError.getStatusCode().is4xxClientError()) {
            // Demais erros de requisição do Spring MVC (parâmetro ausente, tipo inválido etc.).
            return ResponseEntity.status(springError.getStatusCode()).body(ErrorResponse.of("request", "is invalid"));
        }
        log.error("Erro inesperado ao processar a requisição", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("server", "unexpected error"));
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, ApiException ex) {
        return ResponseEntity.status(status).body(ErrorResponse.of(ex.getErrorKey(), ex.getErrorMessage()));
    }

    private static String lastSegment(String fieldPath) {
        int lastDot = fieldPath.lastIndexOf('.');
        return lastDot < 0 ? fieldPath : fieldPath.substring(lastDot + 1);
    }
}
