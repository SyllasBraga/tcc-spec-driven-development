package com.example.blog.shared.error;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
 * Ponto único de conversão de exceções em respostas HTTP no formato
 * {@code {"errors":{"<chave>":["<mensagem>"]}}}.
 *
 * <ul>
 *   <li>{@code 422}: Bean Validation, corpo ilegível e {@link ValidationException};</li>
 *   <li>{@code 409}: {@link ConflictException} e violação de unicidade no banco;</li>
 *   <li>{@code 404}: {@link NotFoundException};</li>
 *   <li>{@code 401}: {@link InvalidCredentialsException} e {@link InvalidTokenException};</li>
 *   <li>demais erros do Spring MVC: o status da própria exceção;</li>
 *   <li>qualquer outra exceção: {@code 500}, registrada em log, sem detalhes ao cliente.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String CANT_BE_BLANK = "can't be blank";
    static final String CANT_BE_EMPTY = "can't be empty";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);
    private static final String CAMPO_RAIZ = "user";
    private static final Map<String, String> CHAVE_POR_RESTRICAO = Map.of(
            "uq_users_username", "username",
            "uq_users_email", "email");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> tratarBeanValidation(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new TreeMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            if (CAMPO_RAIZ.equals(erro.getField())) {
                errors.put("body", new ArrayList<>(List.of(CANT_BE_EMPTY)));
                continue;
            }
            List<String> mensagens = errors.computeIfAbsent(chaveDoCampo(erro.getField()), chave -> new ArrayList<>());
            if (!mensagens.contains(erro.getDefaultMessage())) {
                mensagens.add(erro.getDefaultMessage());
            }
        }
        // Um valor em branco também falha no formato; só a mensagem de ausência interessa ao cliente.
        errors.replaceAll((chave, mensagens) -> mensagens.contains(CANT_BE_BLANK) ? List.of(CANT_BE_BLANK) : mensagens);
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, ErrorResponse.of(errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> tratarCorpoIlegivel(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, ErrorResponse.of("body", CANT_BE_EMPTY));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> tratarValidacao(ValidationException ex) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, ex.body());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> tratarConflito(ConflictException ex) {
        return responder(HttpStatus.CONFLICT, ex.body());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> tratarNaoEncontrado(NotFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.body());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> tratarCredenciaisInvalidas(InvalidCredentialsException ex) {
        return responder(HttpStatus.UNAUTHORIZED, ErrorResponse.of("credentials", "invalid"));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> tratarTokenInvalido(InvalidTokenException ex) {
        return responder(HttpStatus.UNAUTHORIZED, ErrorResponse.of("token", "is invalid"));
    }

    /**
     * Corrida entre duas requisições que passaram pela verificação de unicidade do serviço.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> tratarIntegridade(DataIntegrityViolationException ex) {
        Optional<String> chave = nomeDaRestricao(ex).map(CHAVE_POR_RESTRICAO::get);
        if (chave.isPresent()) {
            return responder(HttpStatus.CONFLICT, new ConflictException(chave.get()).body());
        }
        return erroInesperado(ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> tratarInesperado(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse erroMvc) {
            HttpStatusCode status = erroMvc.getStatusCode();
            return responder(status, ErrorResponse.of("request", motivo(status)));
        }
        return erroInesperado(ex);
    }

    private ResponseEntity<ErrorResponse> erroInesperado(Exception ex) {
        log.error("Erro inesperado ao processar a requisição", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, ErrorResponse.of("server", "unexpected error"));
    }

    /**
     * O charset é declarado aqui porque, ao tratar uma exceção, o Spring MVC reinicia a resposta e o
     * contêiner descarta a codificação definida pelo filtro de encoding (FR-027).
     */
    private static ResponseEntity<ErrorResponse> responder(HttpStatusCode status, ErrorResponse corpo) {
        return ResponseEntity.status(status).contentType(JSON_UTF8).body(corpo);
    }

    private static String chaveDoCampo(String caminho) {
        return caminho.substring(caminho.lastIndexOf('.') + 1);
    }

    private static String motivo(HttpStatusCode status) {
        HttpStatus conhecido = HttpStatus.resolve(status.value());
        return conhecido == null ? "invalid" : conhecido.getReasonPhrase().toLowerCase(Locale.ROOT);
    }

    private static Optional<String> nomeDaRestricao(Throwable ex) {
        for (Throwable atual = ex; atual != null; atual = atual.getCause()) {
            if (atual instanceof ConstraintViolationException violacao && violacao.getConstraintName() != null) {
                return Optional.of(violacao.getConstraintName().toLowerCase(Locale.ROOT));
            }
        }
        return Optional.empty();
    }
}
