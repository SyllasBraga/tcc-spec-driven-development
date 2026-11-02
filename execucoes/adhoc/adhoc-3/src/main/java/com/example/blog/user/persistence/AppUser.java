package com.example.blog.user.persistence;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Usuário cadastrado na plataforma (tabela {@code app_user}). */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(columnDefinition = "text")
    private String image;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {
        // exigido pelo JPA
    }

    public AppUser(String username, String email, String passwordHash, Instant now) {
        this.username = Objects.requireNonNull(username);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.createdAt = Objects.requireNonNull(now);
        this.updatedAt = now;
    }

    public void changeUsername(String newUsername) {
        this.username = Objects.requireNonNull(newUsername);
    }

    public void changeEmail(String newEmail) {
        this.email = Objects.requireNonNull(newEmail);
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash);
    }

    public void changeBio(String newBio) {
        this.bio = newBio;
    }

    public void changeImage(String newImage) {
        this.image = newImage;
    }

    public void markUpdated(Instant now) {
        this.updatedAt = Objects.requireNonNull(now);
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
        return other instanceof AppUser that && id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return AppUser.class.hashCode();
    }
}
