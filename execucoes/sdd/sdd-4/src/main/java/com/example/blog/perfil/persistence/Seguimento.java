package com.example.blog.perfil.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Relação "seguidor segue seguido". Guarda só os ids, sem associações carregadas, para evitar consultas extras.
 */
@Entity
@Table(name = "seguimento")
public class Seguimento {

    @EmbeddedId
    private SeguimentoId id;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Seguimento() {
    }

    public SeguimentoId getId() {
        return id;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof Seguimento seguimento && id != null && id.equals(seguimento.id);
    }

    @Override
    public int hashCode() {
        return Seguimento.class.hashCode();
    }
}
