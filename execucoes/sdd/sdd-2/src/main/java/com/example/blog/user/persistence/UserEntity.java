package com.example.blog.user.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Usuário cadastrado. A senha só é guardada como hash.
 */
@Entity
@Table(name = "users")
public class UserEntity {

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

    protected UserEntity() {
        // exigido pelo JPA
    }

    public UserEntity(String username, String email, String passwordHash, Instant agora) {
        this.username = Objects.requireNonNull(username, "username");
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.createdAt = Objects.requireNonNull(agora, "agora");
        this.updatedAt = agora;
    }

    public void alterarEmail(String novoEmail) {
        this.email = Objects.requireNonNull(novoEmail, "novoEmail");
    }

    public void alterarUsername(String novoUsername) {
        this.username = Objects.requireNonNull(novoUsername, "novoUsername");
    }

    public void alterarSenha(String novoHash) {
        this.passwordHash = Objects.requireNonNull(novoHash, "novoHash");
    }

    public void alterarBio(String novaBio) {
        this.bio = novaBio;
    }

    public void alterarImage(String novaImage) {
        this.image = novaImage;
    }

    public void registrarAtualizacao(Instant agora) {
        this.updatedAt = Objects.requireNonNull(agora, "agora");
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

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof UserEntity usuario)) {
            return false;
        }
        return id != null && id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return UserEntity.class.hashCode();
    }
}
