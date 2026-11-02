package com.example.blog.shared.security;

import java.util.Objects;

import org.springframework.security.oauth2.jwt.Jwt;

import com.example.blog.shared.error.UnauthorizedException;

/**
 * Usuário autenticado na requisição, extraído do JWT já validado.
 *
 * @param id identificador do usuário (claim {@code sub})
 * @param token token apresentado na requisição
 */
public record AuthenticatedUser(long id, String token) {

    public AuthenticatedUser {
        Objects.requireNonNull(token);
    }

    public static AuthenticatedUser from(Jwt jwt) {
        try {
            return new AuthenticatedUser(Long.parseLong(jwt.getSubject()), jwt.getTokenValue());
        } catch (NumberFormatException exception) {
            throw new UnauthorizedException("token", "is invalid");
        }
    }
}
