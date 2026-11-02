package com.example.blog.shared.seguranca;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Lê o token do cabeçalho {@code Authorization: Token <jwt>}, formato exigido pelo contrato Conduit.
 * Cabeçalho ausente resulta em requisição anônima; qualquer outro formato é token inválido.
 */
public class TokenHeaderResolver implements BearerTokenResolver {

    private static final String PREFIXO = "Token ";

    @Override
    public String resolve(HttpServletRequest request) {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho == null) {
            return null;
        }
        if (!cabecalho.startsWith(PREFIXO)) {
            throw new InvalidBearerTokenException("Esquema de autorização não suportado");
        }
        String token = cabecalho.substring(PREFIXO.length()).trim();
        if (token.isEmpty()) {
            throw new InvalidBearerTokenException("Token vazio");
        }
        return token;
    }
}
