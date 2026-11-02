package com.example.blog.profile.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Relação direcional "seguidor → seguido" (tabela {@code follows}). Gravada por
 * {@link FollowRepository#seguir(long, long)}; a entidade existe para o mapeamento e a consulta por id.
 */
@Entity
@Table(name = "follows")
public class Follow {

    @EmbeddedId
    private FollowId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Follow() {
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
        if (!(outro instanceof Follow follow)) {
            return false;
        }
        return id != null && id.equals(follow.id);
    }

    @Override
    public int hashCode() {
        return Follow.class.hashCode();
    }
}
