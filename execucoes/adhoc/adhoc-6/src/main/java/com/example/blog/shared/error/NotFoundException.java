package com.example.blog.shared.error;

import org.springframework.http.HttpStatus;

/**
 * The requested resource does not exist (HTTP 404). The key identifies the resource type.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String resource) {
        super(HttpStatus.NOT_FOUND, resource, "not found");
    }
}
