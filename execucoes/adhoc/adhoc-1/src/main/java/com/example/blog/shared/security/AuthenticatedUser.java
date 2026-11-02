package com.example.blog.shared.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Extrai do JWT autenticado o identificador do usuário, gravado no {@code sub} por {@link TokenService}.
 */
public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    public static long idOf(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
