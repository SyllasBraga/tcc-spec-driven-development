package com.example.blog.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

/**
 * Extrai o JWT do cabeçalho {@code Authorization: Token <jwt>} exigido pelo contrato.
 *
 * <p>Devolve {@code null} (requisição anônima) quando o cabeçalho falta, quando o esquema é outro
 * (por exemplo, {@code Bearer}) e nas rotas de cadastro e login, para que um token antigo não as bloqueie.
 */
@Component
public class TokenAuthorizationHeaderResolver implements BearerTokenResolver {

    private static final String PREFIXO = "Token ";
    private static final Set<String> ROTAS_PUBLICAS_POST = Set.of("/api/users", "/api/users/login");

    @Override
    public String resolve(HttpServletRequest request) {
        if (isRotaPublicaDeCredenciais(request)) {
            return null;
        }
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO)) {
            return null;
        }
        String token = cabecalho.substring(PREFIXO.length()).trim();
        if (token.isEmpty()) {
            throw new InvalidBearerTokenException("Token vazio no cabeçalho Authorization");
        }
        return token;
    }

    private static boolean isRotaPublicaDeCredenciais(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return false;
        }
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        return ROTAS_PUBLICAS_POST.contains(caminho);
    }
}
