package com.example.blog.shared.security;

import java.util.Objects;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Usuário autenticado na requisição atual, extraído do JWT validado.
 *
 * @param id    identificador do usuário (claim {@code sub})
 * @param token token recebido no cabeçalho {@code Authorization}
 */
public record AuthenticatedUser(long id, String token) {

    public AuthenticatedUser {
        Objects.requireNonNull(token, "token");
    }

    public static AuthenticatedUser from(Jwt jwt) {
        Objects.requireNonNull(jwt, "jwt");
        return new AuthenticatedUser(Long.parseLong(jwt.getSubject()), jwt.getTokenValue());
    }
}
