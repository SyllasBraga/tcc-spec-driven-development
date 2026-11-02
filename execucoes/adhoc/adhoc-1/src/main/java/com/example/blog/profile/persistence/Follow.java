package com.example.blog.profile.persistence;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Relação "usuário A segue usuário B". As linhas são gravadas por {@link FollowRepository#insertIfAbsent}.
 */
@Entity
@Table(name = "follows")
public class Follow {

    @EmbeddedId
    private FollowId id;

    protected Follow() {
    }

    public FollowId getId() {
        return id;
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
        return Follow.class.hashCode();
    }
}
