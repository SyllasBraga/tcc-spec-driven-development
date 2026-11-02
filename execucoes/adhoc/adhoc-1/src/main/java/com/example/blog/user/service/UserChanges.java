package com.example.blog.user.service;

/**
 * Alterações pedidas para o usuário autenticado. Cada componente {@code null} significa "campo não enviado".
 * {@code bio} e {@code image} usam {@link ValueChange} porque aceitam {@code null} como novo valor.
 */
public record UserChanges(String email, String username, String password, ValueChange bio, ValueChange image) {

    public boolean isEmpty() {
        return email == null && username == null && password == null && bio == null && image == null;
    }
}
