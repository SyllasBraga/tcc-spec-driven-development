package com.example.blog.shared.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.example.blog.shared.error.ErrorResponse;
import com.example.blog.shared.web.MediaTypes;

import tools.jackson.databind.json.JsonMapper;

/**
 * Responde {@code 401} no formato de erro da API: {@code token: is invalid} quando havia um token recusado e
 * {@code token: is missing} quando não havia token.
 */
@Component
public class ErrorAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    public ErrorAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        String mensagem = tokenRecusado(authException) ? "is invalid" : "is missing";
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaTypes.APPLICATION_JSON_UTF8);
        jsonMapper.writeValue(response.getOutputStream(), ErrorResponse.of("token", mensagem));
    }

    private static boolean tokenRecusado(Throwable ex) {
        for (Throwable atual = ex; atual != null; atual = atual.getCause()) {
            if (atual instanceof OAuth2AuthenticationException) {
                return true;
            }
        }
        return false;
    }
}
