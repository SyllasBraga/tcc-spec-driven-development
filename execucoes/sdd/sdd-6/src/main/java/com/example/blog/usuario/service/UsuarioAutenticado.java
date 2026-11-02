package com.example.blog.usuario.service;

/**
 * Dados públicos do usuário autenticado acompanhados de um token recém-emitido.
 */
public record UsuarioAutenticado(String email, String token, String username, String bio, String image) {

}
