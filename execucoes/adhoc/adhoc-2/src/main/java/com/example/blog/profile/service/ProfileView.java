package com.example.blog.profile.service;

import com.example.blog.user.persistence.User;

/** Perfil público de um usuário, do ponto de vista de quem consulta. */
public record ProfileView(String username, String bio, String image, boolean following) {

    static ProfileView of(User user, boolean following) {
        return new ProfileView(user.getUsername(), user.getBio(), user.getImage(), following);
    }
}
