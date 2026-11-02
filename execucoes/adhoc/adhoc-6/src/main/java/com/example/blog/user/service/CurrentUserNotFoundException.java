package com.example.blog.user.service;

import com.example.blog.shared.error.UnauthorizedException;

/**
 * The token is valid but its user no longer exists, so the request is treated as unauthenticated.
 */
public class CurrentUserNotFoundException extends UnauthorizedException {

    public CurrentUserNotFoundException() {
        super("token", "is invalid");
    }
}
