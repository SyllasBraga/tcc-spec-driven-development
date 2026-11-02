package com.example.blog.profile.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.example.blog.shared.error.ApiException;

/**
 * Não existe usuário com o nome informado.
 */
public class PerfilNaoEncontradoException extends ApiException {

    public PerfilNaoEncontradoException(String username) {
        super(HttpStatus.NOT_FOUND, Map.of("profile", List.of("not found")), "Perfil não encontrado: " + username);
    }
}
