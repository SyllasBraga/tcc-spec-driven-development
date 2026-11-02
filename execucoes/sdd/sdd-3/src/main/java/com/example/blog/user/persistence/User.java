package com.example.blog.user.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Pessoa cadastrada (tabela {@code users}).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "bio")
    private String bio;

    @Column(name = "image")
    private String image;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String username, String email, String passwordHash, Instant agora) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = agora;
        this.updatedAt = agora;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void alterarEmail(String novoEmail) {
        this.email = novoEmail;
    }

    public void alterarUsername(String novoUsername) {
        this.username = novoUsername;
    }

    public void alterarSenha(String novoHash) {
        this.passwordHash = novoHash;
    }

    public void alterarBio(String novaBio) {
        this.bio = novaBio;
    }

    public void alterarImagem(String novaImagem) {
        this.image = novaImagem;
    }

    public void marcarAtualizado(Instant agora) {
        this.updatedAt = agora;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof User usuario)) {
            return false;
        }
        return id != null && id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return User.class.hashCode();
    }
}
