package com.example.blog.shared.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.example.blog.shared.error.ErrorResponse;
import com.example.blog.shared.web.MediaTypes;

import tools.jackson.databind.json.JsonMapper;

/**
 * Responde {@code 403} no formato de erro da API.
 */
@Component
public class ErrorAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public ErrorAccessDeniedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaTypes.APPLICATION_JSON_UTF8);
        jsonMapper.writeValue(response.getOutputStream(), ErrorResponse.of("authorization", "forbidden"));
    }
}
