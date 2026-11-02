package com.example.blog.user.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.example.blog.shared.error.ApiException;

/**
 * {@code PUT /api/user} sem nenhum campo aceito.
 */
public class AtualizacaoVaziaException extends ApiException {

    public AtualizacaoVaziaException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, Map.of("body", List.of("can't be empty")),
                "Atualização sem nenhum campo");
    }
}
