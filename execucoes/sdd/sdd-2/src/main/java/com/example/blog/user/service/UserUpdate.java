package com.example.blog.user.service;

/**
 * Alteração parcial do usuário. Cada campo vem acompanhado de um indicador {@code has*} que diz se ele
 * foi enviado, para distinguir "ausente" (não alterar) de "enviado como nulo" (limpar {@code bio}/{@code image}).
 */
public record UserUpdate(
        boolean hasEmail, String email,
        boolean hasUsername, String username,
        boolean hasPassword, String password,
        boolean hasBio, String bio,
        boolean hasImage, String image) {

    public boolean isVazio() {
        return !hasEmail && !hasUsername && !hasPassword && !hasBio && !hasImage;
    }

    @Override
    public String toString() {
        return "UserUpdate[hasEmail=" + hasEmail + ", hasUsername=" + hasUsername + ", hasPassword=" + hasPassword
                + ", hasBio=" + hasBio + ", hasImage=" + hasImage + "]";
    }
}
