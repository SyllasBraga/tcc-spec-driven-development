package com.example.blog.user.web;

/**
 * Tamanhos aceitos na entrada. Os de nome e e-mail acompanham as colunas da tabela {@code users}.
 */
final class UserLimits {

    static final int USERNAME_MAX = 50;
    static final int EMAIL_MAX = 255;
    static final int PASSWORD_MIN = 8;
    static final int PASSWORD_MAX = 128;
    static final int BIO_MAX = 1000;
    static final int IMAGE_MAX = 2048;

    private UserLimits() {
    }
}
