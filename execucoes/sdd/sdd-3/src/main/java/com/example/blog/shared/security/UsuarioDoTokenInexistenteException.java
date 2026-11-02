package com.example.blog.shared.security;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.example.blog.shared.error.ApiException;

/**
 * O token é válido, mas não identifica um usuário existente.
 */
public class UsuarioDoTokenInexistenteException extends ApiException {

    public UsuarioDoTokenInexistenteException() {
        super(HttpStatus.UNAUTHORIZED, Map.of("token", List.of("is invalid")),
                "Token não identifica um usuário existente");
    }
}
