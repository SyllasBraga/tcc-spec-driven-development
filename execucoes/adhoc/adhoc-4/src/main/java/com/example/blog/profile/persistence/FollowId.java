package com.example.blog.profile.persistence;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Chave composta de {@link Follow}: quem segue e quem é seguido. */
@Embeddable
public class FollowId implements Serializable {

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "followed_id", nullable = false)
    private Long followedId;

    protected FollowId() {
        // exigido pelo JPA
    }

    public FollowId(long followerId, long followedId) {
        this.followerId = followerId;
        this.followedId = followedId;
    }

    public Long getFollowerId() {
        return followerId;
    }

    public Long getFollowedId() {
        return followedId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FollowId that
                && Objects.equals(followerId, that.followerId)
                && Objects.equals(followedId, that.followedId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followerId, followedId);
    }
}
