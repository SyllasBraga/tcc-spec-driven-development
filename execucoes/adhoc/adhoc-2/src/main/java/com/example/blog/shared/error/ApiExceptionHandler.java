package com.example.blog.shared.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.example.blog.shared.web.ApiMediaTypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Ponto único de conversão de exceções em respostas no formato de erro do contrato. */
@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrors> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, List<String>> errors = new TreeMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(lastSegment(fieldError.getField()), key -> new ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }
        exception.getBindingResult().getGlobalErrors().forEach(error ->
                errors.computeIfAbsent("body", key -> new ArrayList<>()).add(error.getDefaultMessage()));
        return respond(HttpStatus.UNPROCESSABLE_CONTENT, new ApiErrors(errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrors> handleUnreadable(HttpMessageNotReadableException exception) {
        return respond(HttpStatus.UNPROCESSABLE_CONTENT, ApiErrors.of("body", "is malformed"));
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiErrors> handleDomain(DomainException exception) {
        HttpStatus status = switch (exception) {
            case NotFoundException ignored -> HttpStatus.NOT_FOUND;
            case ConflictException ignored -> HttpStatus.CONFLICT;
            case UnprocessableException ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
            case UnauthenticatedException ignored -> HttpStatus.UNAUTHORIZED;
        };
        return respond(status, ApiErrors.of(exception.getKey(), exception.getMessage()));
    }

    /** Violação de unicidade em corrida entre duas requisições concorrentes. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrors> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Violação de integridade convertida em conflito: {}", exception.getMostSpecificCause().getMessage());
        return respond(HttpStatus.CONFLICT, ApiErrors.of("resource", "conflicts with existing data"));
    }

    /**
     * Fronteira final: exceções do Spring MVC que já carregam o status adequado (rota inexistente, método ou
     * tipo de mídia não suportado etc.) mantêm esse status; as demais viram 500 sem detalhes internos.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrors> handleOther(Exception exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            String reason = HttpStatus.resolve(status.value()) instanceof HttpStatus known
                    ? known.getReasonPhrase().toLowerCase(Locale.ROOT)
                    : "failed";
            return ResponseEntity.status(status)
                    .headers(errorResponse.getHeaders())
                    .contentType(ApiMediaTypes.JSON_UTF8)
                    .body(ApiErrors.of("request", reason));
        }
        log.error("Erro inesperado ao processar a requisição", exception);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrors.of("server", "internal error"));
    }

    private static ResponseEntity<ApiErrors> respond(HttpStatus status, ApiErrors body) {
        return ResponseEntity.status(status).contentType(ApiMediaTypes.JSON_UTF8).body(body);
    }

    private static String lastSegment(String fieldPath) {
        return fieldPath.substring(fieldPath.lastIndexOf('.') + 1);
    }
}
