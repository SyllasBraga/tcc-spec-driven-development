package com.example.blog.shared.security;

import java.util.Objects;

/**
 * Usuário autenticado na requisição atual, extraído do JWT validado.
 *
 * <p>Pode ser declarado como parâmetro de métodos de controller. Em rotas de autenticação
 * opcional, o parâmetro recebe {@code null} quando a requisição não traz token.
 *
 * @param id    id do usuário (claim {@code sub})
 * @param token token recebido, devolvido nas respostas do tipo User
 */
public record AuthenticatedUser(long id, String token) {

    public AuthenticatedUser {
        Objects.requireNonNull(token);
    }
}
