package com.example.blog.shared.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Lê o token do cabeçalho {@code Authorization: Token <jwt>}, esquema exigido pelo contrato Conduit.
 * Cabeçalho ausente ou com outro esquema é tratado como requisição anônima.
 */
class TokenHeaderBearerTokenResolver implements BearerTokenResolver {

    private static final String SCHEME = "Token ";

    @Override
    public String resolve(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, SCHEME, 0, SCHEME.length())) {
            return null;
        }
        String token = authorization.substring(SCHEME.length()).strip();
        if (token.isEmpty()) {
            throw new InvalidBearerTokenException("Token is empty");
        }
        return token;
    }
}
