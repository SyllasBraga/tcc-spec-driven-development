package com.example.blog.shared.error;

import org.springframework.http.HttpStatus;

/** Falha de autenticação detectada pela aplicação, por exemplo credenciais inválidas no login (401). */
public final class UnauthorizedException extends ApiException {

    public UnauthorizedException(String field, String detail) {
        super(field, detail);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.UNAUTHORIZED;
    }
}
