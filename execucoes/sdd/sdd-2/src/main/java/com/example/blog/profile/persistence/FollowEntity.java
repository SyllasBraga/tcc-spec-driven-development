package com.example.blog.profile.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Relação de seguir. Guarda só os ids, sem associações, para não carregar usuários sem necessidade.
 * A inclusão é feita por {@link FollowRepository#inserirSeAusente}, que é idempotente.
 */
@Entity
@Table(name = "follows")
public class FollowEntity {

    @EmbeddedId
    private FollowId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FollowEntity() {
        // exigido pelo JPA
    }

    public FollowId getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof FollowEntity relacao)) {
            return false;
        }
        return id != null && id.equals(relacao.id);
    }

    @Override
    public int hashCode() {
        return FollowEntity.class.hashCode();
    }
}
