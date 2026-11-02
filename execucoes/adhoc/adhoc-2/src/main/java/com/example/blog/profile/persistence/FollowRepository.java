package com.example.blog.profile.persistence;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    /** Registra a relação; não faz nada se ela já existir, o que torna a operação idempotente. */
    @Modifying
    @Query(value = """
            INSERT INTO follows (follower_id, followed_id, created_at)
            VALUES (:followerId, :followedId, :createdAt)
            ON CONFLICT ON CONSTRAINT pk_follows DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(@Param("followerId") long followerId, @Param("followedId") long followedId,
            @Param("createdAt") Instant createdAt);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.id.followerId = :followerId AND f.id.followedId = :followedId")
    void deleteRelation(@Param("followerId") long followerId, @Param("followedId") long followedId);
}
