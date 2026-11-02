package com.example.blog.user.service;

import com.example.blog.user.persistence.User;

/** Dados do usuário devolvidos pela camada de serviço, sem a senha. */
public record UserData(long id, String username, String email, String bio, String image) {

    static UserData from(User user) {
        return new UserData(user.getId(), user.getUsername(), user.getEmail(), user.getBio(), user.getImage());
    }
}
