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
 * Usuário cadastrado no Conduit. Guarda apenas o hash da senha, nunca a senha em texto puro.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Column(name = "username", nullable = false, length = 50)
	private String username;

	@Column(name = "senha_hash", nullable = false, length = 255)
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

	public Usuario(String email, String username, String senhaHash, Instant agora) {
		this.email = Objects.requireNonNull(email, "email");
		this.username = Objects.requireNonNull(username, "username");
		this.senhaHash = Objects.requireNonNull(senhaHash, "senhaHash");
		this.criadoEm = Objects.requireNonNull(agora, "agora");
		this.atualizadoEm = agora;
	}

	public void alterarEmail(String novoEmail) {
		this.email = Objects.requireNonNull(novoEmail, "novoEmail");
	}

	public void alterarUsername(String novoUsername) {
		this.username = Objects.requireNonNull(novoUsername, "novoUsername");
	}

	public void alterarSenhaHash(String novoSenhaHash) {
		this.senhaHash = Objects.requireNonNull(novoSenhaHash, "novoSenhaHash");
	}

	public void alterarBio(String novaBio) {
		this.bio = novaBio;
	}

	public void alterarImage(String novaImage) {
		this.image = novaImage;
	}

	public void registrarAtualizacao(Instant agora) {
		this.atualizadoEm = Objects.requireNonNull(agora, "agora");
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
