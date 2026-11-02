package com.example.blog.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class TokenHeaderBearerTokenResolverTest {

    private final TokenHeaderBearerTokenResolver resolver = new TokenHeaderBearerTokenResolver();

    @Test
    void deveExtrairTokenDoEsquemaToken() {
        assertThat(resolver.resolve(requestWith("Token abc.def.ghi"))).isEqualTo("abc.def.ghi");
    }

    @Test
    void deveAceitarEsquemaSemDiferenciarMaiusculas() {
        assertThat(resolver.resolve(requestWith("token abc.def.ghi"))).isEqualTo("abc.def.ghi");
    }

    @Test
    void deveTratarCabecalhoAusenteComoAnonimo() {
        assertThat(resolver.resolve(new MockHttpServletRequest())).isNull();
    }

    @Test
    void deveTratarOutroEsquemaComoAnonimo() {
        assertThat(resolver.resolve(requestWith("Bearer abc.def.ghi"))).isNull();
    }

    @Test
    void deveRejeitarTokenVazio() {
        assertThatThrownBy(() -> resolver.resolve(requestWith("Token   ")))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    private static MockHttpServletRequest requestWith(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        return request;
    }
}
