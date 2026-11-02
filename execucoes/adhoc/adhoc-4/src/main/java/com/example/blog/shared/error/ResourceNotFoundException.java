package com.example.blog.shared.error;

import org.springframework.http.HttpStatus;

/** O recurso solicitado não existe (HTTP 404). A chave do erro identifica o tipo do recurso. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource) {
        super(HttpStatus.NOT_FOUND, resource, "not found");
    }
}
