package com.example.blog.shared.error;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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

import com.example.blog.shared.web.ApiMediaTypes;

/**
 * Ponto único de conversão de exceções em respostas no formato {@code {"errors": {...}}} do contrato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorsResponse> handleApiException(ApiException exception) {
        return respond(exception.status(), new ErrorsResponse(exception.errors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorsResponse> handleInvalidArgument(MethodArgumentNotValidException exception) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (ObjectError error : exception.getBindingResult().getAllErrors()) {
            String key = error instanceof FieldError fieldError ? lastSegment(fieldError.getField()) : "body";
            errors.computeIfAbsent(key, ignored -> new ArrayList<>()).add(error.getDefaultMessage());
        }
        return respond(HttpStatus.UNPROCESSABLE_CONTENT, new ErrorsResponse(errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorsResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return respond(HttpStatus.UNPROCESSABLE_CONTENT, ErrorsResponse.of("body", "is invalid JSON"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorsResponse> handleNoResource(NoResourceFoundException exception) {
        return respond(HttpStatus.NOT_FOUND, ErrorsResponse.of("resource", ErrorMessages.NOT_FOUND));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorsResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return respond(exception.getStatusCode(), ErrorsResponse.of("method", "is not supported"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorsResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        return respond(exception.getStatusCode(), ErrorsResponse.of("content type", "is not supported"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorsResponse> handleUnexpected(Exception exception) {
        log.error("Erro inesperado ao processar a requisição", exception);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ErrorsResponse.of("server", "unexpected error"));
    }

    private static ResponseEntity<ErrorsResponse> respond(HttpStatusCode status, ErrorsResponse body) {
        return ResponseEntity.status(status).contentType(ApiMediaTypes.JSON_TYPE).body(body);
    }

    private static String lastSegment(String fieldPath) {
        return fieldPath.substring(fieldPath.lastIndexOf('.') + 1);
    }
}
