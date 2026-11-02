package com.example.blog.user.service;

/**
 * Alterações pedidas para o usuário autenticado.
 *
 * <p>{@code email}, {@code username} e {@code password} nulos significam "não alterar". Para {@code bio} e
 * {@code image}, que aceitam {@code null} como valor, os indicadores {@code bioPresent} e {@code imagePresent}
 * distinguem "não alterar" de "remover". {@code bio} e {@code image} em branco equivalem a {@code null}.
 */
public record UserChanges(
        String email,
        String username,
        String password,
        boolean bioPresent,
        String bio,
        boolean imagePresent,
        String image) {

    public UserChanges {
        bio = blankToNull(bio);
        image = blankToNull(image);
    }

    public boolean isEmpty() {
        return email == null && username == null && password == null && !bioPresent && !imagePresent;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
