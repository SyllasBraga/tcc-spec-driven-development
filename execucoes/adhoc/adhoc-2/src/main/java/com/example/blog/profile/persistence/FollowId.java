package com.example.blog.profile.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Chave da relação "segue".
 *
 * @param followerId id do usuário que segue
 * @param followedId id do usuário seguido
 */
@Embeddable
public record FollowId(
        @Column(name = "follower_id") Long followerId,
        @Column(name = "followed_id") Long followedId) {
}
