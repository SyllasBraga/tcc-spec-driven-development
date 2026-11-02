package com.example.blog.shared.error;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Base class for exceptions that are translated into an API error response with a given status.
 * The error entries are exposed to the client, so they must never contain sensitive data.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, List<String>> errors;

    protected ApiException(HttpStatus status, Map<String, List<String>> errors) {
        super(status.value() + " " + errors);
        this.status = status;
        this.errors = Map.copyOf(new LinkedHashMap<>(errors));
    }

    protected ApiException(HttpStatus status, String key, String message) {
        this(status, Map.of(key, List.of(message)));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }
}
