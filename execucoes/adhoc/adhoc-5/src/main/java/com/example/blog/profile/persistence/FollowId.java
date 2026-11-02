package com.example.blog.profile.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Chave da relação "seguidor segue seguido". */
@Embeddable
public record FollowId(
        @Column(name = "follower_id") long followerId,
        @Column(name = "followed_id") long followedId) {
}
