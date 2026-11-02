package com.example.blog.usuario.web;

import tools.jackson.databind.JsonNode;

/**
 * Corpo do {@code PUT /api/user}. O objeto {@code user} é lido como árvore para distinguir campos ausentes
 * de campos enviados com {@code null}.
 */
public record AtualizacaoRequest(JsonNode user) {
}
