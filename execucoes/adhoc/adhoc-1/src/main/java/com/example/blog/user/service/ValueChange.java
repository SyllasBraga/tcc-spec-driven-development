package com.example.blog.user.service;

/**
 * Novo valor de um campo opcional, que pode ser {@code null} para apagar o valor atual.
 * Serve para distinguir "campo enviado com null" de "campo não enviado".
 */
public record ValueChange(String value) {
}
