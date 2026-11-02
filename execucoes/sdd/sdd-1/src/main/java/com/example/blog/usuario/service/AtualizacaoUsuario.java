package com.example.blog.usuario.service;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Comando de atualização parcial do usuário. Cada campo diz se foi informado e com qual valor,
 * para distinguir campo ausente (manter) de campo informado como {@code null} (limpar).
 */
public record AtualizacaoUsuario(Campo email, Campo username, Campo password, Campo bio, Campo image) {

    public AtualizacaoUsuario {
        Objects.requireNonNull(email, "email é obrigatório");
        Objects.requireNonNull(username, "username é obrigatório");
        Objects.requireNonNull(password, "password é obrigatório");
        Objects.requireNonNull(bio, "bio é obrigatório");
        Objects.requireNonNull(image, "image é obrigatório");
    }

    public boolean hasAlgumCampo() {
        return Stream.of(email, username, password, bio, image).anyMatch(Campo::isInformado);
    }

    /**
     * Valor de um campo do comando.
     *
     * @param isInformado se o campo veio na requisição
     * @param valor       valor informado, possivelmente {@code null}
     */
    public record Campo(boolean isInformado, String valor) {

        private static final Campo AUSENTE = new Campo(false, null);

        public static Campo ausente() {
            return AUSENTE;
        }

        public static Campo informado(String valor) {
            return new Campo(true, valor);
        }
    }
}
