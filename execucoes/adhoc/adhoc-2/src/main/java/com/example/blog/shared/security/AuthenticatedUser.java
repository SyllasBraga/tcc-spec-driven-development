package com.example.blog.shared.security;

import java.util.Objects;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Usuário autenticado na requisição atual.
 *
 * @param id id do usuário, extraído do {@code sub} do token
 * @param token token apresentado na requisição
 */
public record AuthenticatedUser(long id, String token) {

    public AuthenticatedUser {
        Objects.requireNonNull(token, "token");
    }

    public static AuthenticatedUser from(Jwt jwt) {
        return new AuthenticatedUser(Long.parseLong(jwt.getSubject()), jwt.getTokenValue());
    }
}
