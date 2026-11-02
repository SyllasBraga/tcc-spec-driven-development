package com.example.blog.profile.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    /**
     * Grava a relação se ela ainda não existir. É idempotente mesmo com requisições concorrentes.
     */
    @Modifying
    @Query(value = """
            insert into follows (follower_id, followed_id)
            values (:followerId, :followedId)
            on conflict do nothing
            """, nativeQuery = true)
    void insertIfAbsent(long followerId, long followedId);

    /**
     * Remove a relação, se existir.
     */
    @Modifying
    @Query("delete from Follow f where f.id.followerId = :followerId and f.id.followedId = :followedId")
    void deleteIfPresent(long followerId, long followedId);
}
