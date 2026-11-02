package com.example.blog.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class TokenAuthorizationHeaderResolverTest {

    private final TokenAuthorizationHeaderResolver resolver = new TokenAuthorizationHeaderResolver();

    @Test
    void deveExtrairTokenDoEsquemaToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user");
        request.addHeader("Authorization", "Token abc");

        assertThat(resolver.resolve(request)).isEqualTo("abc");
    }

    @Test
    void deveIgnorarEsquemaBearer() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user");
        request.addHeader("Authorization", "Bearer abc");

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void deveDevolverNuloSemCabecalho() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user");

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void deveIgnorarTokenNoLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/login");
        request.addHeader("Authorization", "Token abc");

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void deveIgnorarTokenNoCadastro() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        request.addHeader("Authorization", "Token abc");

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void deveRejeitarEsquemaTokenSemValor() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user");
        request.addHeader("Authorization", "Token   ");

        assertThatThrownBy(() -> resolver.resolve(request)).isInstanceOf(InvalidBearerTokenException.class);
    }
}
