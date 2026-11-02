package com.example.blog.user.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A user following another user. Rows are written by {@link UserFollowRepository} queries.
 */
@Entity
@Table(name = "user_follow")
public class UserFollow {

    @EmbeddedId
    private UserFollowId id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserFollow() {
        // required by JPA
    }

    public UserFollowId getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof UserFollow follow && id != null && id.equals(follow.id));
    }

    @Override
    public int hashCode() {
        return UserFollow.class.hashCode();
    }
}
