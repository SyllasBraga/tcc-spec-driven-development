package com.example.blog.user.service;

/**
 * Partial update of the current user. A {@code null} email, username or password means "keep the current value".
 * Bio and image can be cleared, so whether they were sent is tracked separately from their value.
 */
public record UserChanges(
        String email,
        String username,
        String password,
        boolean bioProvided,
        String bio,
        boolean imageProvided,
        String image) {

    public boolean isEmpty() {
        return email == null && username == null && password == null && !bioProvided && !imageProvided;
    }

    @Override
    public String toString() {
        return "UserChanges[email=" + (email != null) + ", username=" + (username != null)
                + ", password=" + (password != null) + ", bio=" + bioProvided + ", image=" + imageProvided + "]";
    }
}
