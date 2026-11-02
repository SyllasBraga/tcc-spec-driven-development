package com.example.blog.user.web;

import com.example.blog.shared.error.ValidationException;
import com.example.blog.user.service.UserUpdate;
import tools.jackson.databind.JsonNode;

/**
 * Corpo de {@code PUT /api/user}. O objeto {@code user} é lido como árvore JSON para distinguir campo
 * ausente (não alterar) de campo enviado como {@code null} (limpar {@code bio}/{@code image}).
 * Campos desconhecidos são ignorados.
 */
public record UpdateUserRequest(JsonNode user) {

    private static final String CANT_BE_BLANK = "can't be blank";

    /**
     * @throws ValidationException se {@code user} faltar ou não for objeto, se {@code email}, {@code username}
     *                             ou {@code password} não forem texto, ou se {@code bio}/{@code image} não forem
     *                             texto nem {@code null}
     */
    public UserUpdate toUserUpdate() {
        if (user == null || !user.isObject()) {
            throw new ValidationException("body", "can't be empty");
        }
        return new UserUpdate(
                user.has("email"), textoObrigatorio("email"),
                user.has("username"), textoObrigatorio("username"),
                user.has("password"), textoObrigatorio("password"),
                user.has("bio"), textoOpcional("bio"),
                user.has("image"), textoOpcional("image"));
    }

    private String textoObrigatorio(String campo) {
        JsonNode valor = user.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        if (!valor.isString()) {
            throw new ValidationException(campo, CANT_BE_BLANK);
        }
        return valor.stringValue();
    }

    private String textoOpcional(String campo) {
        JsonNode valor = user.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        if (!valor.isString()) {
            throw new ValidationException(campo, "is invalid");
        }
        return valor.stringValue();
    }

    @Override
    public String toString() {
        return "UpdateUserRequest[***]";
    }
}
