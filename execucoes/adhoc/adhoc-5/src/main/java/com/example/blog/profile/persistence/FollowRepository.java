package com.example.blog.profile.persistence;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    /** Registra que um usuário segue outro; se a relação já existir, nada muda (operação idempotente). */
    @Modifying
    @Query(value = """
            INSERT INTO public.user_follows (follower_id, followed_id, created_at)
            VALUES (:followerId, :followedId, :createdAt)
            ON CONFLICT ON CONSTRAINT pk_user_follows DO NOTHING
            """, nativeQuery = true)
    void insertIgnoringDuplicate(@Param("followerId") long followerId, @Param("followedId") long followedId,
            @Param("createdAt") Instant createdAt);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.id = :id")
    void deleteFollow(@Param("id") FollowId id);
}
