package com.example.blog.shared.error;

import org.springframework.http.HttpStatus;

/** Recurso inexistente (404). O campo identifica o tipo do recurso, por exemplo {@code profile}. */
public final class NotFoundException extends ApiException {

    public NotFoundException(String resource) {
        super(resource, "not found");
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.NOT_FOUND;
    }
}
