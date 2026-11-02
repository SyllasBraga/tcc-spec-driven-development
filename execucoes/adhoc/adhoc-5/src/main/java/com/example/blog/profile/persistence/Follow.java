package com.example.blog.profile.persistence;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Um usuário seguindo outro. As inserções são feitas por {@link FollowRepository#insertIgnoringDuplicate},
 * que é idempotente mesmo sob concorrência.
 */
@Entity
@Table(name = "user_follows")
public class Follow {

    @EmbeddedId
    private FollowId id;

    @Column(name = "created_at", nullable = false, updatable = false)
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
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Follow follow && id != null && id.equals(follow.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
