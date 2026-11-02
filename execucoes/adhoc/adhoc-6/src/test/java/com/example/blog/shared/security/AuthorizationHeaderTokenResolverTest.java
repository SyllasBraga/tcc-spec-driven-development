package com.example.blog.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class AuthorizationHeaderTokenResolverTest {

    private final AuthorizationHeaderTokenResolver resolver = new AuthorizationHeaderTokenResolver();

    @Test
    void readsTokenFromTokenScheme() {
        assertThat(resolver.resolve(requestWithAuthorization("Token abc.def.ghi"))).isEqualTo("abc.def.ghi");
    }

    @Test
    void acceptsBearerScheme() {
        assertThat(resolver.resolve(requestWithAuthorization("Bearer abc.def.ghi"))).isEqualTo("abc.def.ghi");
    }

    @Test
    void returnsNullWithoutHeader() {
        assertThat(resolver.resolve(new MockHttpServletRequest())).isNull();
    }

    @Test
    void ignoresOtherSchemes() {
        assertThat(resolver.resolve(requestWithAuthorization("Basic dXNlcjpwYXNz"))).isNull();
    }

    @Test
    void rejectsEmptyToken() {
        assertThatThrownBy(() -> resolver.resolve(requestWithAuthorization("Token   ")))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    private static MockHttpServletRequest requestWithAuthorization(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", value);
        return request;
    }
}
