package com.example.blog.user.service;

import com.example.blog.user.persistence.User;

/** Visão dos dados do usuário devolvida pela camada de serviço. */
public record UserAccount(long id, String email, String username, String bio, String image) {

    static UserAccount from(User user) {
        return new UserAccount(user.getId(), user.getEmail(), user.getUsername(), user.getBio(), user.getImage());
    }
}
