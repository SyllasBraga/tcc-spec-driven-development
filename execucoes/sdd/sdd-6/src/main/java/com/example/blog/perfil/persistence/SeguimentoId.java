package com.example.blog.perfil.persistence;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Chave composta da relação de seguir: quem segue ({@code seguidorId}) e quem é seguido ({@code seguidoId}).
 */
@Embeddable
public class SeguimentoId implements Serializable {

	@Column(name = "seguidor_id", nullable = false)
	private Long seguidorId;

	@Column(name = "seguido_id", nullable = false)
	private Long seguidoId;

	protected SeguimentoId() {
	}

	public SeguimentoId(Long seguidorId, Long seguidoId) {
		this.seguidorId = Objects.requireNonNull(seguidorId, "seguidorId");
		this.seguidoId = Objects.requireNonNull(seguidoId, "seguidoId");
	}

	public Long getSeguidorId() {
		return seguidorId;
	}

	public Long getSeguidoId() {
		return seguidoId;
	}

	@Override
	public boolean equals(Object outro) {
		if (this == outro) {
			return true;
		}
		if (!(outro instanceof SeguimentoId id)) {
			return false;
		}
		return Objects.equals(seguidorId, id.seguidorId) && Objects.equals(seguidoId, id.seguidoId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(seguidorId, seguidoId);
	}

}
