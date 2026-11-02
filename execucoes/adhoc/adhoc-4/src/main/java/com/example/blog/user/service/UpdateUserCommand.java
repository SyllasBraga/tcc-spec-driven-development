package com.example.blog.user.service;

/**
 * Alteração parcial do usuário atual. Campos {@code null} não são alterados.
 * Em {@code bio} e {@code image}, um texto vazio remove o valor atual (a camada web converte o
 * {@code null} explícito do JSON em texto vazio).
 */
public record UpdateUserCommand(String email, String username, String password, String bio, String image) {

    public boolean isEmpty() {
        return email == null && username == null && password == null && bio == null && image == null;
    }
}
