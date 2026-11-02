package com.example.blog.perfil.persistence;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Chave composta do seguimento: quem segue e quem é seguido.
 */
@Embeddable
public class SeguimentoId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "seguidor_id", nullable = false)
    private Long seguidorId;

    @Column(name = "seguido_id", nullable = false)
    private Long seguidoId;

    protected SeguimentoId() {
        // Exigido pelo JPA.
    }

    public SeguimentoId(Long seguidorId, Long seguidoId) {
        this.seguidorId = Objects.requireNonNull(seguidorId, "seguidorId é obrigatório");
        this.seguidoId = Objects.requireNonNull(seguidoId, "seguidoId é obrigatório");
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
