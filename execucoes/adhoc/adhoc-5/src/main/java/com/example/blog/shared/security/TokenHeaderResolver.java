package com.example.blog.shared.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Lê o token do cabeçalho {@code Authorization: Token <jwt>}, formato exigido pelo contrato da API
 * (no lugar do esquema {@code Bearer}).
 */
public class TokenHeaderResolver implements BearerTokenResolver {

    private static final String PREFIXO = "Token ";

    @Override
    public String resolve(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, PREFIXO, 0, PREFIXO.length())) {
            return null;
        }
        String token = header.substring(PREFIXO.length()).strip();
        return token.isEmpty() ? null : token;
    }
}
