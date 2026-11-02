package com.example.blog.shared.config;

import java.nio.charset.StandardCharsets;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Explicita {@code charset=UTF-8} no {@code Content-Type} das respostas JSON, como exige o contrato da API.
 * Sem isso o Spring envia apenas {@code application/json}, pois o UTF-8 é implícito para JSON.
 */
@RestControllerAdvice
public class JsonCharsetResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (MediaType.APPLICATION_JSON.equalsTypeAndSubtype(selectedContentType)
                && selectedContentType.getCharset() == null) {
            response.getHeaders().setContentType(JSON_UTF8);
        }
        return body;
    }
}
