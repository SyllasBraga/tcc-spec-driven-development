package com.example.blog.user.service;

import com.example.blog.user.persistence.User;

/**
 * Dados de um usuário expostos pela camada de serviço, sem o hash da senha.
 */
public record UserData(long id, String email, String username, String bio, String image) {

    static UserData from(User user) {
        return new UserData(user.getId(), user.getEmail(), user.getUsername(), user.getBio(), user.getImage());
    }
}
