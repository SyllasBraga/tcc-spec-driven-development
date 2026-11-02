package com.example.blog.shared.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/** Lê o token do cabeçalho no formato do contrato: {@code Authorization: Token <jwt>}. */
class TokenHeaderResolver implements BearerTokenResolver {

    private static final String SCHEME = "Token ";

    @Override
    public String resolve(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, SCHEME, 0, SCHEME.length())) {
            return null;
        }
        String token = authorization.substring(SCHEME.length()).trim();
        if (token.isEmpty()) {
            throw new InvalidBearerTokenException("Token vazio no cabeçalho Authorization");
        }
        return token;
    }
}
