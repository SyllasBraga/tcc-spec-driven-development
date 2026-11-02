package com.example.blog.user.persistence;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Usuário cadastrado. O e-mail é sempre armazenado em minúsculas.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    private String bio;

    private String image;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String username, String email, String passwordHash, Instant now) {
        this.username = Objects.requireNonNull(username, "username");
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.createdAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
    }

    public void changeUsername(String newUsername, Instant now) {
        this.username = Objects.requireNonNull(newUsername, "newUsername");
        this.updatedAt = now;
    }

    public void changeEmail(String newEmail, Instant now) {
        this.email = Objects.requireNonNull(newEmail, "newEmail");
        this.updatedAt = now;
    }

    public void changePasswordHash(String newPasswordHash, Instant now) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "newPasswordHash");
        this.updatedAt = now;
    }

    /** Altera a biografia; {@code null} remove o valor. */
    public void changeBio(String newBio, Instant now) {
        this.bio = newBio;
        this.updatedAt = now;
    }

    /** Altera a URL da imagem; {@code null} remove o valor. */
    public void changeImage(String newImage, Instant now) {
        this.image = newImage;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
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
