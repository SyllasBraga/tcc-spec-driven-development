package com.example.blog.shared.security;

import com.example.blog.shared.error.InvalidTokenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Obtém o id do usuário autenticado a partir do claim {@code sub} do JWT da requisição atual.
 */
@Component
public class CurrentUserId {

    /**
     * @return o id do usuário autenticado
     * @throws InvalidTokenException se a requisição for anônima ou o {@code sub} não for um id numérico
     */
    public Long exigir() {
        Long id = obterOuNulo();
        if (id == null) {
            throw new InvalidTokenException("Requisição sem usuário autenticado");
        }
        return id;
    }

    /**
     * @return o id do usuário autenticado, ou {@code null} se a requisição for anônima
     * @throws InvalidTokenException se houver JWT, mas o {@code sub} não for um id numérico
     */
    public Long obterOuNulo() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        String sujeito = jwt.getSubject();
        if (sujeito == null) {
            throw new InvalidTokenException("JWT sem claim sub");
        }
        try {
            return Long.valueOf(sujeito);
        } catch (NumberFormatException e) {
            throw new InvalidTokenException("Claim sub não numérico", e);
        }
    }
}
