package com.example.blog.profile.persistence;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Chave composta da relação "seguidor → seguido".
 */
@Embeddable
public record FollowId(
        @Column(name = "follower_id", nullable = false) Long followerId,
        @Column(name = "followed_id", nullable = false) Long followedId) implements Serializable {
}
