package com.example.blog.usuario.web;

import jakarta.validation.constraints.NotNull;

import tools.jackson.databind.node.ObjectNode;

/**
 * Corpo de {@code PUT /api/user}. A raiz {@code user} é mantida como árvore JSON para distinguir campos ausentes
 * de campos com valor {@code null}; a conversão fica em {@link AtualizacaoUsuarioMapper}.
 */
public record AtualizacaoRequest(@NotNull(message = "can't be blank") ObjectNode user) {

}
