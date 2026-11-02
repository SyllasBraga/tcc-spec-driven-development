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
 * Registered user. Email is stored normalized (trimmed, lower case); username is case sensitive.
 */
@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
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

    protected UserAccount() {
        // required by JPA
    }

    public UserAccount(String username, String email, String passwordHash, Instant createdAt) {
        this.username = Objects.requireNonNull(username);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = createdAt;
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

    public void markUpdated(Instant when) {
        this.updatedAt = Objects.requireNonNull(when);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof UserAccount user && id != null && id.equals(user.id));
    }

    @Override
    public int hashCode() {
        return UserAccount.class.hashCode();
    }
}
