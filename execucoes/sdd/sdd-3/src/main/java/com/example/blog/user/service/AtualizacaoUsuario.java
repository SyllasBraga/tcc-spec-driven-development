package com.example.blog.user.service;

/**
 * Campos de atualização do usuário autenticado.
 *
 * <p>{@code null} em qualquer campo significa "não alterar". Em {@code bio} e {@code image}, texto vazio (ou só
 * espaços) significa "limpar".
 */
public record AtualizacaoUsuario(String email, String username, String password, String bio, String image) {

    boolean isVazia() {
        return email == null && username == null && password == null && bio == null && image == null;
    }

    /** Não expõe a senha em logs. */
    @Override
    public String toString() {
        return "AtualizacaoUsuario[username=" + username + "]";
    }
}
