package com.example.blog.profile.persistence;

import jakarta.persistence.Embeddable;

/**
 * Chave de {@link Follow}: quem segue ({@code followerId}) e quem é seguido ({@code followedId}).
 */
@Embeddable
public record FollowId(Long followerId, Long followedId) {
}
