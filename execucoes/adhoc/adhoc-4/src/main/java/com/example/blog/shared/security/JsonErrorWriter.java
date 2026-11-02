package com.example.blog.shared.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.example.blog.shared.error.ErrorResponse;

import tools.jackson.databind.json.JsonMapper;

/** Escreve respostas de erro da camada de segurança no mesmo formato do restante da API. */
@Component
class JsonErrorWriter {

    private final JsonMapper jsonMapper;

    JsonErrorWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    void write(HttpServletResponse response, HttpStatus status, ErrorResponse body) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
