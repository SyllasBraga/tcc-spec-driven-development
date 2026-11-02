package com.example.blog.profile.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Chave composta de {@link Follow}: quem segue e quem é seguido. */
@Embeddable
public record FollowId(
        @Column(name = "follower_id", nullable = false) Long followerId,
        @Column(name = "followed_id", nullable = false) Long followedId) {
}
