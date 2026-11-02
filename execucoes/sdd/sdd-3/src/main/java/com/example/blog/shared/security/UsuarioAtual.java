package com.example.blog.shared.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Identifica o usuário autenticado da requisição a partir do claim {@code sub} do JWT.
 */
@Component
public class UsuarioAtual {

    /**
     * @return id do usuário autenticado
     * @throws UsuarioDoTokenInexistenteException se a requisição não tiver JWT ou o {@code sub} não for numérico
     */
    public long idObrigatorio() {
        return idOpcional().orElseThrow(UsuarioDoTokenInexistenteException::new);
    }

    /**
     * @return id do usuário autenticado, ou vazio em requisição anônima
     * @throws UsuarioDoTokenInexistenteException se houver JWT com {@code sub} não numérico
     */
    public Optional<Long> idOpcional() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(jwt.getSubject()));
        } catch (NumberFormatException e) {
            throw new UsuarioDoTokenInexistenteException();
        }
    }
}
