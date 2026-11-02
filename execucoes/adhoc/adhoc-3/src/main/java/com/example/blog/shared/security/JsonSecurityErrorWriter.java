package com.example.blog.shared.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.example.blog.shared.error.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Escreve as respostas 401 e 403 da camada de segurança no formato de erro do contrato,
 * já que elas acontecem nos filtros, antes do {@code @RestControllerAdvice}.
 */
class JsonSecurityErrorWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String TOKEN_SCHEME = "Token";

    private final JsonMapper jsonMapper;

    JsonSecurityErrorWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        boolean tokenWasSent = authException instanceof InvalidBearerTokenException;
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, TOKEN_SCHEME);
        write(response, HttpStatus.UNAUTHORIZED, ErrorResponse.of("token", tokenWasSent ? "is invalid" : "is missing"));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, ErrorResponse.of("access", "is forbidden"));
    }

    private void write(HttpServletResponse response, HttpStatus status, ErrorResponse body) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
