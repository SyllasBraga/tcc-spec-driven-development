package com.example.blog.perfil.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Relação de seguir entre dois usuários, mapeada só pelos ids (sem associações).
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

    public Seguimento(SeguimentoId id, Instant criadoEm) {
        this.id = Objects.requireNonNull(id, "id");
        this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm");
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
        return outro instanceof Seguimento seguimento && id.equals(seguimento.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
