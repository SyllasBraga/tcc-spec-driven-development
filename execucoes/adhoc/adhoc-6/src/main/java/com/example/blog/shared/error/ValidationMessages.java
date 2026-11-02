package com.example.blog.shared.error;

/**
 * Validation messages exposed by the API, shared by Bean Validation annotations and services.
 */
public final class ValidationMessages {

    public static final String BLANK = "can't be blank";
    public static final String EMPTY = "can't be empty";
    public static final String INVALID = "is invalid";
    public static final String TAKEN = "has already been taken";
    public static final String TOO_LONG = "is too long";

    private ValidationMessages() {
    }
}
