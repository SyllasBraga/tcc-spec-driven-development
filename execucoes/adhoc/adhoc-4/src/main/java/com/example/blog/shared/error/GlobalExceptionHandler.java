package com.example.blog.shared.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

/** Converte as exceções da aplicação no formato de erro do contrato, em um único ponto. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        return respond(exception.getStatus(), ErrorResponse.of(exception.getField(), exception.getDetail()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, List<String>> errors = new TreeMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            addError(errors, lastPathSegment(error.getField()), error.getDefaultMessage());
        }
        for (ObjectError error : exception.getBindingResult().getGlobalErrors()) {
            addError(errors, "body", error.getDefaultMessage());
        }
        return respond(HttpStatus.UNPROCESSABLE_CONTENT, new ErrorResponse(errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return respond(HttpStatus.UNPROCESSABLE_CONTENT, ErrorResponse.of(invalidFieldName(exception), "is invalid"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorResponse.of("body", "must be application/json"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, ErrorResponse.of("method", "is not allowed"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException exception) {
        return respond(HttpStatus.NOT_FOUND, ErrorResponse.of("resource", "not found"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Erro inesperado ao processar a requisição", exception);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ErrorResponse.of("server", "unexpected error"));
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, ErrorResponse body) {
        return ResponseEntity.status(status).body(body);
    }

    private static void addError(Map<String, List<String>> errors, String field, String message) {
        errors.computeIfAbsent(field, key -> new ArrayList<>()).add(message);
    }

    /**
     * Campo do JSON com tipo errado (por exemplo, número onde se espera texto), ou {@code body} quando o problema
     * não é de um campo específico (JSON malformado, raiz com tipo errado).
     */
    private static String invalidFieldName(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof MismatchedInputException mismatch) {
            List<JacksonException.Reference> path = mismatch.getPath();
            if (!path.isEmpty() && path.getLast().getPropertyName() != null) {
                return path.getLast().getPropertyName();
            }
        }
        return "body";
    }

    /** Converte caminhos como {@code user.email} no nome do campo do contrato ({@code email}). */
    private static String lastPathSegment(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }
}
