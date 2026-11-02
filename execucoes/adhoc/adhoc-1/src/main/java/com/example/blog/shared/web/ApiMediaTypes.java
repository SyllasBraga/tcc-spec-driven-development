package com.example.blog.shared.web;

import org.springframework.http.MediaType;

/**
 * Tipos de mídia exigidos pelo contrato da API, que pede {@code charset=utf-8} explícito no {@code Content-Type}.
 */
public final class ApiMediaTypes {

    public static final String JSON = "application/json;charset=UTF-8";

    public static final MediaType JSON_TYPE = MediaType.parseMediaType(JSON);

    private ApiMediaTypes() {
    }
}
