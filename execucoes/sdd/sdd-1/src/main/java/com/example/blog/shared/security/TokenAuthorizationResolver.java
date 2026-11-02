package com.example.blog.shared.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Lê o token do cabeçalho {@code Authorization: Token <jwt>}, esquema exigido pelo contrato.
 * Cabeçalho ausente ou com outro esquema (por exemplo, {@code Bearer}) é tratado como requisição sem token.
 */
public class TokenAuthorizationResolver implements BearerTokenResolver {

    private static final String PREFIXO = "Token ";

    @Override
    public String resolve(HttpServletRequest request) {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho == null || !cabecalho.regionMatches(true, 0, PREFIXO, 0, PREFIXO.length())) {
            return null;
        }
        String token = cabecalho.substring(PREFIXO.length()).trim();
        if (token.isEmpty()) {
            throw new InvalidBearerTokenException("Token vazio");
        }
        return token;
    }
}
