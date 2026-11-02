package com.example.blog.user.service;

/**
 * Alteração parcial do usuário autenticado.
 *
 * <p>{@code email}, {@code username} e {@code password} nulos significam "não alterar".
 * Como {@code bio} e {@code image} aceitam {@code null} como valor, a presença de cada um
 * é indicada à parte: {@code bioPresent = true} com {@code bio = null} (ou texto em branco) apaga a bio.
 */
public record UserUpdate(
        String email,
        String username,
        String password,
        boolean bioPresent,
        String bio,
        boolean imagePresent,
        String image) {

    /** Indica que nenhum campo foi informado. */
    public boolean isEmpty() {
        return email == null && username == null && password == null && !bioPresent && !imagePresent;
    }
}
