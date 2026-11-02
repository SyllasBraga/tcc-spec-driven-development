package com.example.blog.shared.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class ConduitTokenResolverTest {

	private final ConduitTokenResolver resolver = new ConduitTokenResolver();

	@Test
	void deveRetornarNuloQuandoNaoHaCabecalho() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		assertThat(resolver.resolve(request)).isNull();
	}

	@Test
	void deveExtrairTokenQuandoPrefixoEToken() {
		MockHttpServletRequest request = comAutorizacao("Token abc.def.ghi");

		assertThat(resolver.resolve(request)).isEqualTo("abc.def.ghi");
	}

	@Test
	void deveRejeitarPrefixoBearer() {
		MockHttpServletRequest request = comAutorizacao("Bearer abc");

		assertThatThrownBy(() -> resolver.resolve(request)).isInstanceOf(InvalidBearerTokenException.class);
	}

	@Test
	void deveRejeitarTokenVazio() {
		MockHttpServletRequest request = comAutorizacao("Token ");

		assertThatThrownBy(() -> resolver.resolve(request)).isInstanceOf(InvalidBearerTokenException.class);
	}

	@Test
	void deveRejeitarCabecalhoVazio() {
		MockHttpServletRequest request = comAutorizacao("");

		assertThatThrownBy(() -> resolver.resolve(request)).isInstanceOf(InvalidBearerTokenException.class);
	}

	private static MockHttpServletRequest comAutorizacao(String valor) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, valor);
		return request;
	}

}
