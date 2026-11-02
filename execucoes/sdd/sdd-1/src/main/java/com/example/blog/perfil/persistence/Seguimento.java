package com.example.blog.perfil.persistence;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Relação "usuário A segue usuário B". Guarda só os ids, sem relação JPA com a entidade de usuário.
 */
@Entity
@Table(name = "seguidor", schema = "public")
public class Seguimento {

    @EmbeddedId
    private SeguimentoId id;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Seguimento() {
        // Exigido pelo JPA.
    }

    public Seguimento(SeguimentoId id, Instant agora) {
        this.id = Objects.requireNonNull(id, "id é obrigatório");
        this.criadoEm = Objects.requireNonNull(agora, "agora é obrigatório");
    }

    public SeguimentoId getId() {
        return id;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Seguimento seguimento)) {
            return false;
        }
        return id != null && id.equals(seguimento.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
