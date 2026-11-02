package com.example.blog.user.web;

import com.example.blog.user.service.UserData;

public record UserResponse(UserView user) {

    static UserResponse of(UserData user, String token) {
        return new UserResponse(UserView.of(user, token));
    }
}
