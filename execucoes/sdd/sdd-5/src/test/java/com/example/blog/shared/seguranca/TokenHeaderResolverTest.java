package com.example.blog.shared.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class TokenHeaderResolverTest {

    private final TokenHeaderResolver resolver = new TokenHeaderResolver();

    @Test
    void deveDevolverNuloSemCabecalho() {
        assertThat(resolver.resolve(new MockHttpServletRequest())).isNull();
    }

    @Test
    void deveExtrairTokenDoEsquemaToken() {
        assertThat(resolver.resolve(requisicaoCom("Token abc"))).isEqualTo("abc");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer abc", "Token ", "Token", "abc", "token abc"})
    void deveRejeitarOutrosFormatos(String cabecalho) {
        assertThatThrownBy(() -> resolver.resolve(requisicaoCom(cabecalho)))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    private static MockHttpServletRequest requisicaoCom(String cabecalho) {
        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.addHeader(HttpHeaders.AUTHORIZATION, cabecalho);
        return requisicao;
    }
}
