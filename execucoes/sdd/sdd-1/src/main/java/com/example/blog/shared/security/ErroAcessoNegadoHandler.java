package com.example.blog.shared.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Responde {@code 403} no formato de erro do contrato.
 */
public class ErroAcessoNegadoHandler implements AccessDeniedHandler {

    static final String CORPO_ACESSO_NEGADO = "{\"errors\":{\"authorization\":[\"forbidden\"]}}";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        ErroAutenticacaoEntryPoint.RespostaJson.escrever(
                response, HttpServletResponse.SC_FORBIDDEN, CORPO_ACESSO_NEGADO);
    }
}
