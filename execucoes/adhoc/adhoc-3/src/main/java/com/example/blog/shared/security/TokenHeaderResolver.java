package com.example.blog.shared.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Lê o token do cabeçalho {@code Authorization: Token <jwt>}, esquema exigido pelo contrato Conduit
 * no lugar do {@code Bearer} padrão. Cabeçalho ausente ou com outro esquema equivale a não autenticado.
 */
class TokenHeaderResolver implements BearerTokenResolver {

    private static final String SCHEME_PREFIX = "Token ";

    @Override
    public String resolve(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, SCHEME_PREFIX, 0, SCHEME_PREFIX.length())) {
            return null;
        }
        String token = header.substring(SCHEME_PREFIX.length()).strip();
        return token.isEmpty() ? null : token;
    }
}
