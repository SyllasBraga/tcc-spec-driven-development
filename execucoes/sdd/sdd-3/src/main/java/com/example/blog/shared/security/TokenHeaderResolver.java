package com.example.blog.shared.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Lê o token do cabeçalho {@code Authorization: Token <jwt>}, o único formato aceito pelo contrato.
 *
 * <p>Sem cabeçalho, a requisição segue anônima. Cabeçalho com outro esquema ou sem valor é recusado como token
 * inválido.
 */
public class TokenHeaderResolver implements BearerTokenResolver {

    private static final String PREFIXO = "Token ";

    @Override
    public String resolve(HttpServletRequest request) {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho == null) {
            return null;
        }
        if (cabecalho.startsWith(PREFIXO)) {
            String token = cabecalho.substring(PREFIXO.length()).strip();
            if (!token.isEmpty()) {
                return token;
            }
        }
        throw new OAuth2AuthenticationException(
                new BearerTokenError("invalid_token", HttpStatus.UNAUTHORIZED, "Invalid token header", null));
    }
}
