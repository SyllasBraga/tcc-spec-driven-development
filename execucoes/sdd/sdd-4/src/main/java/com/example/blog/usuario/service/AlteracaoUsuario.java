package com.example.blog.usuario.service;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Alteração parcial do usuário atual. Cada campo distingue "ausente" (não alterar) de "presente", inclusive
 * presente com valor {@code null} (apagar {@code bio} ou {@code image}).
 */
public record AlteracaoUsuario(Campo email, Campo username, Campo password, Campo bio, Campo image) {

    public AlteracaoUsuario {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(bio, "bio");
        Objects.requireNonNull(image, "image");
    }

    /** {@code true} quando nenhum campo foi informado. */
    public boolean isVazia() {
        return Stream.of(email, username, password, bio, image).noneMatch(Campo::presente);
    }

    /** Valor opcional de um campo, com indicação explícita de presença. */
    public record Campo(boolean presente, String valor) {

        public static final Campo AUSENTE = new Campo(false, null);

        public static Campo com(String valor) {
            return new Campo(true, valor);
        }
    }
}
