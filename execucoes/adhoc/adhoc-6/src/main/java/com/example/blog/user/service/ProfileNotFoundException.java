package com.example.blog.user.service;

import com.example.blog.shared.error.NotFoundException;

public class ProfileNotFoundException extends NotFoundException {

    public ProfileNotFoundException() {
        super("profile");
    }
}
