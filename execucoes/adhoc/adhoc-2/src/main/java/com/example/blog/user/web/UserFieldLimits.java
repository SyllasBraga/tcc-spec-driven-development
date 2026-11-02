package com.example.blog.user.web;

/** Tamanhos máximos dos campos do usuário, alinhados às colunas do banco e ao limite do BCrypt. */
final class UserFieldLimits {

    static final int EMAIL = 255;
    static final int USERNAME = 255;
    /** O BCrypt considera no máximo 72 bytes da senha. */
    static final int PASSWORD = 72;

    private UserFieldLimits() {
    }
}
