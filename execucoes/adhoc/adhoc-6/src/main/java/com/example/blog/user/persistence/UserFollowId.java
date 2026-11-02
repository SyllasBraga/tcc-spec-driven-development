package com.example.blog.user.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Composite key of {@link UserFollow}: who follows whom.
 */
@Embeddable
public record UserFollowId(
        @Column(name = "follower_id", nullable = false) Long followerId,
        @Column(name = "followed_id", nullable = false) Long followedId) {
}
