package com.example.blog.profile.persistence;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Relação "segue" entre dois usuários. */
@Entity
@Table(name = "follows")
public class Follow {

    @EmbeddedId
    private FollowId id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Follow() {
        // exigido pelo JPA
    }

    public Follow(FollowId id, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public FollowId getId() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Follow follow && id.equals(follow.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
