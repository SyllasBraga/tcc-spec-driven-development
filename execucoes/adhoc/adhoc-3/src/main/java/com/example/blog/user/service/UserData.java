package com.example.blog.user.service;

import com.example.blog.user.persistence.AppUser;

/** Dados de um usuário expostos pela camada de serviço (nunca a entidade JPA). */
public record UserData(long id, String email, String username, String bio, String image) {

    static UserData from(AppUser user) {
        return new UserData(user.getId(), user.getEmail(), user.getUsername(), user.getBio(), user.getImage());
    }
}
