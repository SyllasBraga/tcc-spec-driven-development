package com.example.blog.user.service;

import com.example.blog.shared.error.UnauthenticatedException;

/** E-mail ou senha incorretos no login. A mensagem não revela qual dos dois falhou. */
public class InvalidCredentialsException extends UnauthenticatedException {

    public InvalidCredentialsException() {
        super("credentials", "invalid");
    }
}
