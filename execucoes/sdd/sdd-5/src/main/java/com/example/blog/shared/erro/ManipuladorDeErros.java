package com.example.blog.shared.erro;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
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

/**
 * Ponto único de conversão de exceções em respostas {@code {"errors":{...}}} do contrato.
 */
@RestControllerAdvice
public class ManipuladorDeErros {

    private static final Logger log = LoggerFactory.getLogger(ManipuladorDeErros.class);
    private static final String PREFIXO_USER = MensagensErro.CHAVE_USER + ".";
    private static final HttpStatus ENTIDADE_NAO_PROCESSAVEL = HttpStatus.UNPROCESSABLE_CONTENT;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrosResponse> tratarValidacao(MethodArgumentNotValidException excecao) {
        Map<String, TreeSet<String>> agrupados = new TreeMap<>();
        for (FieldError erro : excecao.getBindingResult().getFieldErrors()) {
            if (MensagensErro.CHAVE_USER.equals(erro.getField())) {
                return responder(ENTIDADE_NAO_PROCESSAVEL,
                        ErrosResponse.de(MensagensErro.CHAVE_BODY, MensagensErro.CORPO_VAZIO));
            }
            String campo = erro.getField().startsWith(PREFIXO_USER)
                    ? erro.getField().substring(PREFIXO_USER.length())
                    : erro.getField();
            agrupados.computeIfAbsent(campo, chave -> new TreeSet<>()).add(erro.getDefaultMessage());
        }
        Map<String, List<String>> erros = new TreeMap<>();
        agrupados.forEach((campo, mensagens) -> erros.put(campo, priorizarVazio(mensagens)));
        return responder(ENTIDADE_NAO_PROCESSAVEL, new ErrosResponse(erros));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrosResponse> tratarCorpoIlegivel(HttpMessageNotReadableException excecao) {
        return responder(ENTIDADE_NAO_PROCESSAVEL,
                ErrosResponse.de(MensagensErro.CHAVE_BODY, MensagensErro.CORPO_VAZIO));
    }

    @ExceptionHandler(ValidacaoException.class)
    ResponseEntity<ErrosResponse> tratarValidacaoDeRegra(ValidacaoException excecao) {
        return responder(ENTIDADE_NAO_PROCESSAVEL, new ErrosResponse(excecao.getErros()));
    }

    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<ErrosResponse> tratarConflito(ConflitoException excecao) {
        return responder(HttpStatus.CONFLICT, new ErrosResponse(excecao.getErros()));
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    ResponseEntity<ErrosResponse> tratarCredenciaisInvalidas(CredenciaisInvalidasException excecao) {
        return responder(HttpStatus.UNAUTHORIZED,
                ErrosResponse.de(MensagensErro.CHAVE_CREDENCIAIS, MensagensErro.CREDENCIAIS_INVALIDAS));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErrosResponse> tratarNaoEncontrado(RecursoNaoEncontradoException excecao) {
        return responder(HttpStatus.NOT_FOUND, ErrosResponse.de(excecao.getChave(), MensagensErro.NAO_ENCONTRADO));
    }

    @ExceptionHandler(TokenInvalidoException.class)
    ResponseEntity<ErrosResponse> tratarTokenInvalido(TokenInvalidoException excecao) {
        return responder(HttpStatus.UNAUTHORIZED,
                ErrosResponse.de(MensagensErro.CHAVE_TOKEN, MensagensErro.INVALIDO));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrosResponse> tratarInesperado(Exception excecao) {
        if (excecao instanceof ErrorResponse erroDoFramework) {
            return tratarErroDoFramework(erroDoFramework.getStatusCode());
        }
        log.error("Erro inesperado ao processar a requisição", excecao);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrosResponse.de(MensagensErro.CHAVE_SERVIDOR, MensagensErro.ERRO_INTERNO));
    }

    private ResponseEntity<ErrosResponse> tratarErroDoFramework(HttpStatusCode status) {
        HttpStatus conhecido = HttpStatus.resolve(status.value());
        String motivo = conhecido == null ? "error" : conhecido.getReasonPhrase().toLowerCase(Locale.ROOT);
        return responder(status, ErrosResponse.de(MensagensErro.CHAVE_REQUISICAO, motivo));
    }

    /** Campo vazio gera só "can't be blank", mesmo que outra regra (como formato de e-mail) também falhe. */
    private static List<String> priorizarVazio(TreeSet<String> mensagens) {
        if (mensagens.contains(MensagensErro.NAO_PODE_SER_VAZIO)) {
            return List.of(MensagensErro.NAO_PODE_SER_VAZIO);
        }
        return new ArrayList<>(mensagens);
    }

    private static ResponseEntity<ErrosResponse> responder(HttpStatusCode status, ErrosResponse corpo) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(corpo);
    }
}
