package com.example.blog.user.web;

import com.example.blog.user.service.UserAccount;

/** Usuário autenticado no formato do contrato. */
public record UserBody(String email, String token, String username, String bio, String image) {

    static UserBody from(UserAccount account, String token) {
        return new UserBody(account.email(), token, account.username(), account.bio(), account.image());
    }
}
