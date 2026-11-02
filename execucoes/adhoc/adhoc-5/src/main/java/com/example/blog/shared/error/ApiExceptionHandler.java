package com.example.blog.shared.error;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Ponto único de conversão de exceções em respostas no formato de erro do contrato da API.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        return respond(exception.status(), ErrorResponse.of(exception.field(), exception.detail()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, List<String>> errors = new TreeMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(simpleFieldName(fieldError.getField()), key -> new ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }
        if (errors.isEmpty()) {
            errors.put("body", List.of("is invalid"));
        }
        return respond(HttpStatus.UNPROCESSABLE_CONTENT, new ErrorResponse(errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        log.debug("Corpo da requisição ilegível", exception);
        return respond(HttpStatus.UNPROCESSABLE_CONTENT, ErrorResponse.of("body", "can't be empty or malformed"));
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatusCode status, ErrorResponse body) {
        return ResponseEntity.status(status).contentType(JSON_UTF8).body(body);
    }

    /** Converte {@code user.email} em {@code email}: o envelope {@code user} não interessa ao cliente. */
    private static String simpleFieldName(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }
}
