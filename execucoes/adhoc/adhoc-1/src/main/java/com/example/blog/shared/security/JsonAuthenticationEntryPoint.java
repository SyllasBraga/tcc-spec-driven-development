package com.example.blog.shared.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.example.blog.shared.error.ErrorsResponse;
import com.example.blog.shared.web.ApiMediaTypes;

import tools.jackson.databind.json.JsonMapper;

/**
 * Responde 401 no formato de erro do contrato, distinguindo token ausente de token inválido.
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    public JsonAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        String message = exception instanceof OAuth2AuthenticationException ? "is invalid" : "is missing";
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Token");
        response.setContentType(ApiMediaTypes.JSON);
        jsonMapper.writeValue(response.getOutputStream(), ErrorsResponse.of("token", message));
    }
}
