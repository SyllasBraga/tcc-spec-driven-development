package com.example.blog.shared.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.example.blog.shared.error.ErrorResponse;

import tools.jackson.databind.json.JsonMapper;

/**
 * Writes authentication (401) and authorization (403) failures in the error format of the API contract.
 */
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String TOKEN_SCHEME = "Token";

    private final JsonMapper jsonMapper;

    public JsonSecurityErrorHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        boolean tokenRejected = exception instanceof OAuth2AuthenticationException;
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, TOKEN_SCHEME);
        write(response, HttpStatus.UNAUTHORIZED, ErrorResponse.of("token", tokenRejected ? "is invalid" : "is missing"));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        write(response, HttpStatus.FORBIDDEN, ErrorResponse.of("authorization", "is not allowed"));
    }

    private void write(HttpServletResponse response, HttpStatus status, ErrorResponse body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
