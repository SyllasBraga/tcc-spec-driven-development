package com.example.blog.shared.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Lê o token do cabeçalho {@code Authorization: Token <jwt>}, esquema exigido pelo contrato.
 *
 * <p>Sem cabeçalho, a requisição segue anônima ({@code null}); com qualquer outro formato, o token é
 * rejeitado com {@link InvalidBearerTokenException}.
 */
public class TokenHeaderResolver implements BearerTokenResolver {

    private static final String PREFIXO = "Token ";

    @Override
    public String resolve(HttpServletRequest requisicao) {
        String cabecalho = requisicao.getHeader(HttpHeaders.AUTHORIZATION);
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
