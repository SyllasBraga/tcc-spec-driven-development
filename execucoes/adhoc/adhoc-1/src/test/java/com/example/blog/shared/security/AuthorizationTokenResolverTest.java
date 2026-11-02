package com.example.blog.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class AuthorizationTokenResolverTest {

    private final AuthorizationTokenResolver resolver = new AuthorizationTokenResolver();

    @Test
    void deveLerTokenDoEsquemaToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Token abc.def.ghi");

        assertThat(resolver.resolve(request)).isEqualTo("abc.def.ghi");
    }

    @Test
    void deveRetornarNuloSemCabecalho() {
        assertThat(resolver.resolve(new MockHttpServletRequest())).isNull();
    }

    @Test
    void deveIgnorarOutroEsquema() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc.def.ghi");

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void deveRetornarNuloQuandoTokenVazio() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Token   ");

        assertThat(resolver.resolve(request)).isNull();
    }
}
