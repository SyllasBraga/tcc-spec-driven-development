package com.example.blog.profile.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Chave da relação "seguidor segue seguido".
 */
@Embeddable
public class FollowId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "followed_id", nullable = false)
    private Long followedId;

    protected FollowId() {
        // exigido pelo JPA
    }

    public FollowId(Long followerId, Long followedId) {
        this.followerId = Objects.requireNonNull(followerId, "followerId");
        this.followedId = Objects.requireNonNull(followedId, "followedId");
    }

    public Long getFollowerId() {
        return followerId;
    }

    public Long getFollowedId() {
        return followedId;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof FollowId id)) {
            return false;
        }
        return Objects.equals(followerId, id.followerId) && Objects.equals(followedId, id.followedId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followerId, followedId);
    }
}
