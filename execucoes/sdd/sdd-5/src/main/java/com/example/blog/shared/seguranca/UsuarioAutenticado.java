package com.example.blog.shared.seguranca;

import com.example.blog.shared.erro.TokenInvalidoException;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Extrai o id do usuário autenticado da claim {@code sub} do token.
 */
public final class UsuarioAutenticado {

    private UsuarioAutenticado() {
    }

    /**
     * @param jwt token já validado pelo resource server
     * @return id do usuário
     * @throws TokenInvalidoException se o token não tiver um id numérico
     */
    public static Long id(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new TokenInvalidoException();
        }
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException excecao) {
            throw new TokenInvalidoException();
        }
    }

    /**
     * Variante para rotas de autenticação opcional.
     *
     * @return id do usuário, ou {@code null} se a requisição for anônima
     */
    public static Long idOuNulo(Jwt jwt) {
        return jwt == null ? null : id(jwt);
    }
}
