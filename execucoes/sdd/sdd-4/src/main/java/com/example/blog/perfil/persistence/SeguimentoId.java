package com.example.blog.perfil.persistence;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Chave composta de {@link Seguimento}: quem segue e quem é seguido. */
@Embeddable
public class SeguimentoId implements Serializable {

    @Column(name = "seguidor_id", nullable = false)
    private long seguidorId;

    @Column(name = "seguido_id", nullable = false)
    private long seguidoId;

    protected SeguimentoId() {
    }

    public SeguimentoId(long seguidorId, long seguidoId) {
        this.seguidorId = seguidorId;
        this.seguidoId = seguidoId;
    }

    public long getSeguidorId() {
        return seguidorId;
    }

    public long getSeguidoId() {
        return seguidoId;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof SeguimentoId id && seguidorId == id.seguidorId && seguidoId == id.seguidoId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(seguidorId, seguidoId);
    }
}
