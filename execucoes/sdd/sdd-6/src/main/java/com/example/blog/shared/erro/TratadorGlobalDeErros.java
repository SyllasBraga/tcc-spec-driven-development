package com.example.blog.shared.erro;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Ponto único de conversão de exceções em respostas HTTP no formato
 * {@code {"errors": {"<chave>": ["<mensagem>"]}}} definido pelo contrato.
 */
@RestControllerAdvice
public class TratadorGlobalDeErros {

	private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeErros.class);

	private static final HttpStatus UNPROCESSABLE = HttpStatus.UNPROCESSABLE_CONTENT;

	private static final MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

	private static final String CHAVE_CORPO = "body";

	private static final String PREFIXO_RAIZ = "user.";

	private static final String CAMPO_RAIZ = "user";

	private static final String MENSAGEM_INVALIDO = "is invalid";

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrosResponse> tratarValidacao(MethodArgumentNotValidException excecao) {
		Map<String, List<String>> erros = new LinkedHashMap<>();
		for (FieldError erro : excecao.getBindingResult().getFieldErrors()) {
			erros.computeIfAbsent(chaveDoCampo(erro), chave -> new ArrayList<>()).add(mensagemDe(erro));
		}
		if (erros.isEmpty()) {
			erros.put(CHAVE_CORPO, List.of(MENSAGEM_INVALIDO));
		}
		return responder(UNPROCESSABLE, new ErrosResponse(erros));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrosResponse> tratarCorpoIlegivel(HttpMessageNotReadableException excecao) {
		return responder(UNPROCESSABLE, ErrosResponse.de(CHAVE_CORPO, MENSAGEM_INVALIDO));
	}

	@ExceptionHandler(ValidacaoException.class)
	ResponseEntity<ErrosResponse> tratarValidacaoDeDominio(ValidacaoException excecao) {
		return responder(UNPROCESSABLE, excecao.paraResposta());
	}

	@ExceptionHandler(CampoJaUtilizadoException.class)
	ResponseEntity<ErrosResponse> tratarCampoJaUtilizado(CampoJaUtilizadoException excecao) {
		return responder(HttpStatus.CONFLICT, excecao.paraResposta());
	}

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	ResponseEntity<ErrosResponse> tratarNaoEncontrado(RecursoNaoEncontradoException excecao) {
		return responder(HttpStatus.NOT_FOUND, excecao.paraResposta());
	}

	@ExceptionHandler(NaoAutenticadoException.class)
	ResponseEntity<ErrosResponse> tratarNaoAutenticado(NaoAutenticadoException excecao) {
		return responder(HttpStatus.UNAUTHORIZED, excecao.paraResposta());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ErrosResponse> tratarViolacaoDeIntegridade(DataIntegrityViolationException excecao) {
		String detalhe = String.valueOf(excecao.getMostSpecificCause().getMessage());
		String chave;
		if (detalhe.contains("uq_usuario_email")) {
			chave = "email";
		}
		else if (detalhe.contains("uq_usuario_username")) {
			chave = "username";
		}
		else {
			chave = CHAVE_CORPO;
		}
		return responder(HttpStatus.CONFLICT, ErrosResponse.de(chave, CampoJaUtilizadoException.MENSAGEM));
	}

	/**
	 * Exceções do próprio Spring MVC que já carregam um status (rota inexistente, método não suportado etc.).
	 */
	@ExceptionHandler({ ServletException.class, ErrorResponseException.class })
	ResponseEntity<ErrosResponse> tratarErroDoFramework(Exception excecao) {
		if (!(excecao instanceof ErrorResponse erroComStatus)) {
			return tratarInesperado(excecao);
		}
		HttpStatusCode status = erroComStatus.getStatusCode();
		HttpStatus conhecido = HttpStatus.resolve(status.value());
		String mensagem = conhecido != null ? conhecido.getReasonPhrase().toLowerCase(Locale.ROOT) : MENSAGEM_INVALIDO;
		return responder(status, ErrosResponse.de("request", mensagem));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrosResponse> tratarInesperado(Exception excecao) {
		log.error("Erro inesperado ao processar a requisição", excecao);
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, ErrosResponse.de("server", "unexpected error"));
	}

	/**
	 * Define o {@code Content-Type} com charset explicitamente, pois o Spring MVC limpa a resposta antes de tratar
	 * exceções e o charset configurado no filtro de codificação se perde.
	 */
	private static ResponseEntity<ErrosResponse> responder(HttpStatusCode status, ErrosResponse corpo) {
		return ResponseEntity.status(status).contentType(JSON_UTF8).body(corpo);
	}

	private static String chaveDoCampo(FieldError erro) {
		String campo = erro.getField();
		if (CAMPO_RAIZ.equals(campo)) {
			return CHAVE_CORPO;
		}
		return campo.startsWith(PREFIXO_RAIZ) ? campo.substring(PREFIXO_RAIZ.length()) : campo;
	}

	private static String mensagemDe(FieldError erro) {
		if (CAMPO_RAIZ.equals(erro.getField())) {
			return MENSAGEM_INVALIDO;
		}
		return erro.getDefaultMessage() != null ? erro.getDefaultMessage() : MENSAGEM_INVALIDO;
	}

}
