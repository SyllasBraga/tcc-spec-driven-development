package com.example.blog.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

class TokenHeaderResolverTest {

    private final TokenHeaderResolver resolver = new TokenHeaderResolver();

    @Test
    void deveDevolverNuloSemCabecalho() {
        assertThat(resolver.resolve(new MockHttpServletRequest())).isNull();
    }

    @Test
    void deveExtrairTokenComPrefixoToken() {
        assertThat(resolver.resolve(comCabecalho("Token abc.def.ghi"))).isEqualTo("abc.def.ghi");
    }

    @Test
    void deveRecusarEsquemaBearer() {
        assertThatThrownBy(() -> resolver.resolve(comCabecalho("Bearer abc")))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void deveRecusarTokenVazio() {
        assertThatThrownBy(() -> resolver.resolve(comCabecalho("Token ")))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    private static MockHttpServletRequest comCabecalho(String valor) {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", valor);
        return request;
    }
}
