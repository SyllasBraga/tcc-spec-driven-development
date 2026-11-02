package com.example.blog.usuario.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Conta de usuário do Conduit. O hash da senha nunca deve sair da camada de serviço.
 */
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

    @Column(name = "imagem")
    private String imagem;

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

    public void alterarEmail(String novoEmail) {
        this.email = Objects.requireNonNull(novoEmail, "email");
    }

    public void alterarUsername(String novoUsername) {
        this.username = Objects.requireNonNull(novoUsername, "username");
    }

    public void alterarSenhaHash(String novoSenhaHash) {
        this.senhaHash = Objects.requireNonNull(novoSenhaHash, "senhaHash");
    }

    public void alterarBio(String novaBio) {
        this.bio = novaBio;
    }

    public void alterarImagem(String novaImagem) {
        this.imagem = novaImagem;
    }

    public void registrarAlteracao(Instant agora) {
        this.atualizadoEm = Objects.requireNonNull(agora, "agora");
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

    public String getImagem() {
        return imagem;
    }

    public Instant getCriadoEm() {
        return criadoEm;
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
