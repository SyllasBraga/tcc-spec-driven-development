package com.example.blog.shared.web;

import org.springframework.http.MediaType;

/** Tipo de mídia das respostas da API, com charset explícito como pede o contrato Conduit. */
public final class ApiMediaTypes {

    public static final String JSON_UTF8_VALUE = "application/json;charset=UTF-8";
    public static final MediaType JSON_UTF8 = MediaType.parseMediaType(JSON_UTF8_VALUE);

    private ApiMediaTypes() {
    }
}
