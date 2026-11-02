package com.example.blog.shared.error;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * The request conflicts with existing state, such as a unique value already in use (HTTP 409).
 */
public class ConflictException extends ApiException {

    public ConflictException(Map<String, List<String>> errors) {
        super(HttpStatus.CONFLICT, errors);
    }
}
