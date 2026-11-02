package com.example.blog.user.service;

import com.example.blog.shared.error.UnauthorizedException;

/**
 * Login failed. The message does not reveal whether the email exists.
 */
public class InvalidCredentialsException extends UnauthorizedException {

    public InvalidCredentialsException() {
        super("credentials", "invalid");
    }
}
