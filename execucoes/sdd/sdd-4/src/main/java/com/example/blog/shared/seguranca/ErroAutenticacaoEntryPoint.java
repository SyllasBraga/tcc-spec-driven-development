package com.example.blog.shared.seguranca;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.example.blog.shared.erro.ErroResponse;

import tools.jackson.databind.json.JsonMapper;

/** Responde 401 no formato de erro do contrato: token ausente ({@code is missing}) ou inválido ({@code is invalid}). */
public class ErroAutenticacaoEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    public ErroAutenticacaoEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException excecao) throws IOException {
        String mensagem = request.getHeader(HttpHeaders.AUTHORIZATION) == null ? "is missing" : "is invalid";
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(ErroResponse.TIPO_DE_CONTEUDO.toString());
        jsonMapper.writeValue(response.getOutputStream(), ErroResponse.de("token", mensagem));
    }
}
