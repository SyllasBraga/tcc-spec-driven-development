package com.example.blog.user.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.example.blog.shared.error.ApiException;

/**
 * E-mail inexistente ou senha incorreta no login. Não indica qual dos dois falhou.
 */
public class CredenciaisInvalidasException extends ApiException {

    public CredenciaisInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, Map.of("credentials", List.of("invalid")), "Credenciais inválidas");
    }
}
