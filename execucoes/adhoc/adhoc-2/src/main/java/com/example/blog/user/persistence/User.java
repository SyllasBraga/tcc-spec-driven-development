package com.example.blog.user.persistence;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Usuário cadastrado. O e-mail é sempre gravado em minúsculas. */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String bio;

    private String image;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String email, String username, String passwordHash, Instant now) {
        this.email = Objects.requireNonNull(email, "email");
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.createdAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getBio() {
        return bio;
    }

    public String getImage() {
        return image;
    }

    public void changeEmail(String newEmail) {
        this.email = Objects.requireNonNull(newEmail, "newEmail");
    }

    public void changeUsername(String newUsername) {
        this.username = Objects.requireNonNull(newUsername, "newUsername");
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "newPasswordHash");
    }

    /** Altera a biografia; {@code null} a remove. */
    public void changeBio(String newBio) {
        this.bio = newBio;
    }

    /** Altera a imagem; {@code null} a remove. */
    public void changeImage(String newImage) {
        this.image = newImage;
    }

    public void markUpdated(Instant now) {
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof User user && id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return User.class.hashCode();
    }
}
