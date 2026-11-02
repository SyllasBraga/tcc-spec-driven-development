package com.example.blog.shared.seguranca;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

/**
 * Extrai o JWT do cabeçalho {@code Authorization: Token <jwt>}, o único formato aceito pelo contrato.
 * <p>
 * Sem cabeçalho, devolve {@code null} e a requisição segue anônima. Qualquer outro formato, inclusive
 * {@code Bearer}, gera {@link InvalidBearerTokenException}, que vira {@code 401}.
 */
@Component
public class ConduitTokenResolver implements BearerTokenResolver {

	private static final String PREFIXO = "Token ";

	@Override
	public String resolve(HttpServletRequest request) {
		String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (cabecalho == null) {
			return null;
		}
		if (!cabecalho.startsWith(PREFIXO)) {
			throw new InvalidBearerTokenException("Authorization header must use the Token scheme");
		}
		String token = cabecalho.substring(PREFIXO.length()).trim();
		if (token.isEmpty()) {
			throw new InvalidBearerTokenException("Token is empty");
		}
		return token;
	}

}
