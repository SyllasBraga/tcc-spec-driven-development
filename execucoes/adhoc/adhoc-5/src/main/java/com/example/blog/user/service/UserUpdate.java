package com.example.blog.user.service;

/**
 * Alteração parcial do usuário. Campo {@code null} significa "não alterar".
 * Para {@code bio} e {@code image}, texto vazio remove o valor atual.
 */
public record UserUpdate(String email, String username, String password, String bio, String image) {

    public boolean isEmpty() {
        return email == null && username == null && password == null && bio == null && image == null;
    }
}
