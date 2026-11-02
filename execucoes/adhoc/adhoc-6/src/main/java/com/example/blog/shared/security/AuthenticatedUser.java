package com.example.blog.shared.security;

/**
 * Principal of an authenticated request: the user id from the token subject and the raw token.
 *
 * @param id user id
 * @param token JWT sent by the client, echoed back in user responses
 */
public record AuthenticatedUser(long id, String token) {

    @Override
    public String toString() {
        return "AuthenticatedUser[id=" + id + "]";
    }
}
