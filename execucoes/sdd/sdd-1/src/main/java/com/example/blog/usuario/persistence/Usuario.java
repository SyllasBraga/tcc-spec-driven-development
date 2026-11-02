package com.example.blog.usuario.persistence;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Conta da plataforma. E-mail gravado normalizado (minúsculas, sem espaços nas pontas);
 * a senha é guardada somente como hash.
 */
@Entity
@Table(name = "usuario", schema = "public")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @Column(name = "image", columnDefinition = "text")
    private String image;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Usuario() {
        // Exigido pelo JPA.
    }

    public Usuario(String email, String username, String senhaHash, Instant agora) {
        this.email = Objects.requireNonNull(email, "email é obrigatório");
        this.username = Objects.requireNonNull(username, "username é obrigatório");
        this.senhaHash = Objects.requireNonNull(senhaHash, "senhaHash é obrigatório");
        this.criadoEm = Objects.requireNonNull(agora, "agora é obrigatório");
        this.atualizadoEm = agora;
    }

    public void alterarEmail(String novoEmail, Instant agora) {
        this.email = Objects.requireNonNull(novoEmail, "email é obrigatório");
        this.atualizadoEm = agora;
    }

    public void alterarUsername(String novoUsername, Instant agora) {
        this.username = Objects.requireNonNull(novoUsername, "username é obrigatório");
        this.atualizadoEm = agora;
    }

    public void alterarSenhaHash(String novoSenhaHash, Instant agora) {
        this.senhaHash = Objects.requireNonNull(novoSenhaHash, "senhaHash é obrigatório");
        this.atualizadoEm = agora;
    }

    /** Altera a bio; {@code null} limpa o campo. */
    public void alterarBio(String novaBio, Instant agora) {
        this.bio = novaBio;
        this.atualizadoEm = agora;
    }

    /** Altera a imagem; {@code null} limpa o campo. */
    public void alterarImage(String novaImage, Instant agora) {
        this.image = novaImage;
        this.atualizadoEm = agora;
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

    public String getSenhaHash() {
        return senhaHash;
    }

    public String getBio() {
        return bio;
    }

    public String getImage() {
        return image;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
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
