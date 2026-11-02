package com.example.blog.usuario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.usuario.service.Alteracao;
import com.example.blog.usuario.service.AtualizacaoUsuario;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class AtualizacaoUsuarioMapperTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static ValidatorFactory fabricaDeValidadores;

	private static AtualizacaoUsuarioMapper mapper;

	@BeforeAll
	static void criarMapper() {
		fabricaDeValidadores = Validation.buildDefaultValidatorFactory();
		mapper = new AtualizacaoUsuarioMapper(fabricaDeValidadores.getValidator());
	}

	@AfterAll
	static void fecharFabrica() {
		fabricaDeValidadores.close();
	}

	@Test
	void campoAusenteDeveVirarManter() {
		AtualizacaoUsuario resultado = mapper.converter(json("{\"bio\":\"x\"}"));

		assertThat(resultado.email()).isEqualTo(Alteracao.manter());
		assertThat(resultado.username()).isEqualTo(Alteracao.manter());
		assertThat(resultado.senha()).isEqualTo(Alteracao.manter());
		assertThat(resultado.image()).isEqualTo(Alteracao.manter());
	}

	@Test
	void bioNulaDeveVirarDefinirNulo() {
		AtualizacaoUsuario resultado = mapper.converter(json("{\"bio\":null}"));

		assertThat(resultado.bio()).isEqualTo(Alteracao.definir(null));
	}

	@Test
	void bioVaziaDeveVirarDefinirNulo() {
		AtualizacaoUsuario resultado = mapper.converter(json("{\"bio\":\"\"}"));

		assertThat(resultado.bio()).isEqualTo(Alteracao.definir(null));
	}

	@Test
	void imageSoComEspacosDeveVirarDefinirNulo() {
		AtualizacaoUsuario resultado = mapper.converter(json("{\"image\":\"   \"}"));

		assertThat(resultado.image()).isEqualTo(Alteracao.definir(null));
	}

	@Test
	void imageComTextoDeveVirarDefinir() {
		AtualizacaoUsuario resultado = mapper.converter(json("{\"image\":\"x\"}"));

		assertThat(resultado.image()).isEqualTo(Alteracao.definir("x"));
	}

	@Test
	void emailValidoDeveSerAparado() {
		AtualizacaoUsuario resultado = mapper.converter(json("{\"email\":\" jake@example.com \"}"));

		assertThat(resultado.email()).isEqualTo(Alteracao.definir("jake@example.com"));
	}

	@Test
	void objetoVazioDeveSerRecusado() {
		assertValidacao("{}", "body", "at least one field is required");
	}

	@Test
	void objetoSomenteComCamposDesconhecidosDeveSerRecusado() {
		assertValidacao("{\"token\":\"x\"}", "body", "at least one field is required");
	}

	@Test
	void emailNuloDeveSerRecusado() {
		assertValidacao("{\"email\":null}", "email", "can't be blank");
	}

	@Test
	void emailVazioDeveSerRecusado() {
		assertValidacao("{\"email\":\"\"}", "email", "can't be blank");
	}

	@Test
	void emailInvalidoDeveSerRecusado() {
		assertValidacao("{\"email\":\"invalido\"}", "email", "is invalid");
	}

	@Test
	void senhaVaziaDeveSerRecusada() {
		assertValidacao("{\"password\":\"\"}", "password", "can't be blank");
	}

	@Test
	void usernameLongoDeveSerRecusado() {
		assertValidacao("{\"username\":\"" + "a".repeat(51) + "\"}", "username", "is too long");
	}

	@Test
	void emailSemPontoNoDominioDeveSerRecusado() {
		assertValidacao("{\"email\":\"a@b\"}", "email", "is invalid");
	}

	@Test
	void valorNaoTextualDeveSerRecusado() {
		assertValidacao("{\"email\":123}", "email", "is invalid");
	}

	@Test
	void bioNaoTextualDeveSerRecusada() {
		assertValidacao("{\"bio\":true}", "bio", "is invalid");
	}

	private static void assertValidacao(String corpo, String chave, String mensagem) {
		ObjectNode user = json(corpo);

		assertThatThrownBy(() -> mapper.converter(user)).isInstanceOfSatisfying(ValidacaoException.class,
				excecao -> {
					assertThat(excecao.getChave()).isEqualTo(chave);
					assertThat(excecao.getMensagem()).isEqualTo(mensagem);
				});
	}

	private static ObjectNode json(String corpo) {
		return (ObjectNode) JSON.readTree(corpo);
	}

}
