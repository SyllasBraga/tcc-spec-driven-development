package com.example.blog.user.web;

import static com.example.blog.shared.error.ValidationMessages.BLANK;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.example.blog.shared.error.UnprocessableException;
import com.example.blog.user.service.UserUpdate;

/** Corpo de {@code PUT /api/user}. */
public record UpdateUserRequest(@NotNull(message = BLANK) @Valid UpdateUserFields user) {

    /** @throws UnprocessableException se email, username ou password vierem explicitamente como {@code null} */
    UserUpdate toUserUpdate() {
        user.explicitNullFields().stream().findFirst().ifPresent(field -> {
            throw new UnprocessableException(field, BLANK);
        });
        return new UserUpdate(
                user.getEmail(),
                user.getUsername(),
                user.getPassword(),
                user.isBioPresent(),
                user.getBio(),
                user.isImagePresent(),
                user.getImage());
    }
}
