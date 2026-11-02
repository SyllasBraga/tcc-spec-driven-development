package com.example.blog.usuario.persistence;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Usuário cadastrado na plataforma. A senha é guardada somente como hash. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(name = "bio")
    private String bio;

    @Column(name = "image")
    private String image;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Usuario() {
    }

    public Usuario(String username, String email, String senhaHash, Instant agora) {
        this.username = Objects.requireNonNull(username, "username");
        this.email = Objects.requireNonNull(email, "email");
        this.senhaHash = Objects.requireNonNull(senhaHash, "senhaHash");
        this.criadoEm = Objects.requireNonNull(agora, "agora");
        this.atualizadoEm = agora;
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

    public String getSenhaHash() {
        return senhaHash;
    }

    public String getBio() {
        return bio;
    }

    public String getImage() {
        return image;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void alterarEmail(String novoEmail, Instant agora) {
        this.email = Objects.requireNonNull(novoEmail, "novoEmail");
        this.atualizadoEm = agora;
    }

    public void alterarUsername(String novoUsername, Instant agora) {
        this.username = Objects.requireNonNull(novoUsername, "novoUsername");
        this.atualizadoEm = agora;
    }

    public void alterarSenhaHash(String novoHash, Instant agora) {
        this.senhaHash = Objects.requireNonNull(novoHash, "novoHash");
        this.atualizadoEm = agora;
    }

    public void alterarBio(String novaBio, Instant agora) {
        this.bio = novaBio;
        this.atualizadoEm = agora;
    }

    public void alterarImage(String novaImage, Instant agora) {
        this.image = novaImage;
        this.atualizadoEm = agora;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Usuario usuario)) {
            return false;
        }
        return id != null && id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return Usuario.class.hashCode();
    }
}
