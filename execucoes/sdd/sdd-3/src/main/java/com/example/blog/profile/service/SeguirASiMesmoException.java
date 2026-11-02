package com.example.blog.profile.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.example.blog.shared.error.ApiException;

/**
 * O usuário autenticado tentou seguir o próprio perfil.
 */
public class SeguirASiMesmoException extends ApiException {

    public SeguirASiMesmoException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, Map.of("profile", List.of("cannot follow yourself")),
                "Usuário tentou seguir a si mesmo");
    }
}
