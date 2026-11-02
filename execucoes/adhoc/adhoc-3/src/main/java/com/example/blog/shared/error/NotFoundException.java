package com.example.blog.shared.error;

/** Recurso inexistente (HTTP 404). A chave identifica o tipo do recurso, por exemplo {@code profile}. */
public class NotFoundException extends ApiException {

    private static final String NOT_FOUND = "not found";

    public NotFoundException(String resource) {
        super(resource, NOT_FOUND);
    }
}
