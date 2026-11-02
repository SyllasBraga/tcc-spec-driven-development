package com.example.blog.profile.service;

import com.example.blog.user.persistence.User;

/**
 * Perfil público de um usuário, do ponto de vista de quem consulta ({@code following}).
 */
public record ProfileData(String username, String bio, String image, boolean following) {

    static ProfileData of(User user, boolean following) {
        return new ProfileData(user.getUsername(), user.getBio(), user.getImage(), following);
    }
}
