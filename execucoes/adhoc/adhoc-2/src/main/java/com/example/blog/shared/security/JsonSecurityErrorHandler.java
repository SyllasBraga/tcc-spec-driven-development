package com.example.blog.shared.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.example.blog.shared.error.ApiErrors;
import com.example.blog.shared.web.ApiMediaTypes;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import tools.jackson.databind.json.JsonMapper;

/** Responde às falhas de autenticação (401) e de autorização (403) no formato de erro do contrato. */
class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String AUTH_SCHEME = "Token";

    private final JsonMapper jsonMapper;

    JsonSecurityErrorHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        String message = exception instanceof OAuth2AuthenticationException ? "is invalid" : "is missing";
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, AUTH_SCHEME);
        write(response, HttpStatus.UNAUTHORIZED, ApiErrors.of("token", message));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        write(response, HttpStatus.FORBIDDEN, ApiErrors.of("request", "forbidden"));
    }

    private void write(HttpServletResponse response, HttpStatus status, ApiErrors body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(ApiMediaTypes.JSON_UTF8_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
