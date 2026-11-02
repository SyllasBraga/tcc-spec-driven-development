package com.example.blog.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class TokenHeaderResolverTest {

    private final TokenHeaderResolver resolver = new TokenHeaderResolver();

    @Test
    void deveExtrairTokenDoEsquemaToken() {
        assertThat(resolver.resolve(requestWithAuthorization("Token abc.def.ghi"))).isEqualTo("abc.def.ghi");
    }

    @Test
    void deveAceitarEsquemaSemDiferenciarMaiusculas() {
        assertThat(resolver.resolve(requestWithAuthorization("token abc.def.ghi"))).isEqualTo("abc.def.ghi");
    }

    @Test
    void deveIgnorarEsquemaBearer() {
        assertThat(resolver.resolve(requestWithAuthorization("Bearer abc.def.ghi"))).isNull();
    }

    @Test
    void deveRetornarNuloQuandoTokenVazio() {
        assertThat(resolver.resolve(requestWithAuthorization("Token   "))).isNull();
    }

    @Test
    void deveRetornarNuloSemCabecalho() {
        assertThat(resolver.resolve(new MockHttpServletRequest())).isNull();
    }

    private static MockHttpServletRequest requestWithAuthorization(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", value);
        return request;
    }
}
