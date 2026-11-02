package com.example.blog.usuario.web;

import tools.jackson.databind.node.ObjectNode;

/**
 * Corpo de {@code PUT /api/user}. O objeto {@code user} é lido como árvore JSON para distinguir
 * campo ausente de campo informado como {@code null}.
 */
public record AtualizacaoUsuarioRequest(ObjectNode user) {
}
