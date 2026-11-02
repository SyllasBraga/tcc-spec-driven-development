package com.example.blog.usuario.service;

/**
 * Campo de uma atualização parcial: distingue "não enviado" de "enviado com valor (possivelmente nulo)".
 *
 * @param presente se o campo veio na requisição
 * @param valor valor enviado; pode ser {@code null} quando presente
 * @param <T> tipo do valor
 */
public record CampoAtualizado<T>(boolean presente, T valor) {

    private static final CampoAtualizado<?> AUSENTE = new CampoAtualizado<>(false, null);

    public CampoAtualizado {
        if (!presente && valor != null) {
            throw new IllegalArgumentException("Campo ausente não pode ter valor");
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> CampoAtualizado<T> ausente() {
        return (CampoAtualizado<T>) AUSENTE;
    }

    public static <T> CampoAtualizado<T> de(T valor) {
        return new CampoAtualizado<>(true, valor);
    }
}
