package com.example.blog.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

class TokenHeaderResolverTest {

    private final TokenHeaderResolver resolver = new TokenHeaderResolver();

    @Test
    void deveExtrairTokenDoEsquemaToken() {
        assertThat(resolver.resolve(requestWith("Token abc.def.ghi"))).isEqualTo("abc.def.ghi");
    }

    @Test
    void deveIgnorarEsquemaBearer() {
        assertThat(resolver.resolve(requestWith("Bearer abc.def.ghi"))).isNull();
    }

    @Test
    void deveTratarTokenVazioComoAusente() {
        assertThat(resolver.resolve(requestWith("Token   "))).isNull();
    }

    @Test
    void deveRetornarNuloSemCabecalho() {
        assertThat(resolver.resolve(new MockHttpServletRequest())).isNull();
    }

    private static MockHttpServletRequest requestWith(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        return request;
    }
}
