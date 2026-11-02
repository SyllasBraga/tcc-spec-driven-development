package com.example.blog.usuario.web;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.usuario.service.Alteracao;
import com.example.blog.usuario.service.AtualizacaoUsuario;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Converte a raiz {@code user} de {@code PUT /api/user} em {@link AtualizacaoUsuario}, distinguindo campo
 * ausente de campo com valor {@code null}.
 * <p>
 * Regras: só {@code email}, {@code username}, {@code password}, {@code bio} e {@code image} são considerados, e
 * ao menos um deles é exigido. {@code email}, {@code username} e {@code password} não aceitam nulo nem vazio e
 * seguem as mesmas restrições do cadastro; {@code bio} e {@code image} aceitam texto ou {@code null}, e texto
 * vazio ou só com espaços é gravado como {@code null}.
 */
@Component
public class AtualizacaoUsuarioMapper {

	private static final String EMAIL = "email";

	private static final String USERNAME = "username";

	private static final String PASSWORD = "password";

	private static final String BIO = "bio";

	private static final String IMAGE = "image";

	private static final List<String> CAMPOS_ACEITOS = List.of(EMAIL, USERNAME, PASSWORD, BIO, IMAGE);

	private static final String VAZIO = "can't be blank";

	private static final String INVALIDO = "is invalid";

	private final Validator validator;

	public AtualizacaoUsuarioMapper(Validator validator) {
		this.validator = validator;
	}

	/**
	 * @throws ValidacaoException se nenhum campo aceito for informado ou se algum valor for inválido
	 */
	public AtualizacaoUsuario converter(ObjectNode user) {
		if (CAMPOS_ACEITOS.stream().noneMatch(user::has)) {
			throw new ValidacaoException("body", "at least one field is required");
		}
		return new AtualizacaoUsuario(obrigatorio(user, EMAIL), obrigatorio(user, USERNAME),
				obrigatorio(user, PASSWORD), opcional(user, BIO), opcional(user, IMAGE));
	}

	private Alteracao<String> obrigatorio(ObjectNode user, String campo) {
		if (!user.has(campo)) {
			return Alteracao.manter();
		}
		JsonNode valor = user.get(campo);
		if (valor.isNull()) {
			throw new ValidacaoException(campo, VAZIO);
		}
		if (!valor.isString()) {
			throw new ValidacaoException(campo, INVALIDO);
		}
		String texto = PASSWORD.equals(campo) ? valor.stringValue() : valor.stringValue().trim();
		validar(campo, texto);
		return Alteracao.definir(texto);
	}

	private static Alteracao<String> opcional(ObjectNode user, String campo) {
		if (!user.has(campo)) {
			return Alteracao.manter();
		}
		JsonNode valor = user.get(campo);
		if (valor.isNull()) {
			return Alteracao.definir(null);
		}
		if (!valor.isString()) {
			throw new ValidacaoException(campo, INVALIDO);
		}
		String texto = valor.stringValue();
		return Alteracao.definir(texto.isBlank() ? null : texto);
	}

	/**
	 * Reaplica as restrições declaradas em {@link CadastroRequest.Dados}, para que cadastro e atualização sigam
	 * exatamente as mesmas regras. Com mais de uma violação, prevalece a de "vazio", depois a ordem alfabética.
	 */
	private void validar(String campo, String valor) {
		Set<ConstraintViolation<CadastroRequest.Dados>> violacoes =
				validator.validateValue(CadastroRequest.Dados.class, campo, valor);
		violacoes.stream()
			.map(ConstraintViolation::getMessage)
			.min(Comparator.comparing((String mensagem) -> !VAZIO.equals(mensagem))
				.thenComparing(Comparator.naturalOrder()))
			.ifPresent(mensagem -> {
				throw new ValidacaoException(campo, mensagem);
			});
	}

}
