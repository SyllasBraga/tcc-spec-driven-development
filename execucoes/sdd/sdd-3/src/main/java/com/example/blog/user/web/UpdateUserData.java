package com.example.blog.user.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

import com.example.blog.user.service.AtualizacaoUsuario;

import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Campos opcionais de atualização. Campo ausente chega como {@code null} (não alterar) e {@code null} explícito
 * chega como {@code ""}: em {@code bio} e {@code image} isso limpa o valor; em {@code email}, {@code username} e
 * {@code password}, que não podem ficar vazios, a validação recusa com {@code 422}.
 */
public record UpdateUserData(
        @JsonDeserialize(using = NuloComoVazioDeserializer.class)
        @Pattern(regexp = NAO_VAZIO, message = "can't be blank") @Email(message = "is invalid") String email,
        @JsonDeserialize(using = NuloComoVazioDeserializer.class)
        @Pattern(regexp = NAO_VAZIO, message = "can't be blank") String username,
        @JsonDeserialize(using = NuloComoVazioDeserializer.class)
        @Pattern(regexp = NAO_VAZIO, message = "can't be blank") String password,
        @JsonDeserialize(using = NuloComoVazioDeserializer.class) String bio,
        @JsonDeserialize(using = NuloComoVazioDeserializer.class) String image) {

    /** Ao menos um caractere que não seja espaço. */
    static final String NAO_VAZIO = "(?s).*\\S.*";

    AtualizacaoUsuario paraAtualizacao() {
        return new AtualizacaoUsuario(email, username, password, bio, image);
    }

    /** Não expõe a senha em logs. */
    @Override
    public String toString() {
        return "UpdateUserData[username=" + username + "]";
    }
}
