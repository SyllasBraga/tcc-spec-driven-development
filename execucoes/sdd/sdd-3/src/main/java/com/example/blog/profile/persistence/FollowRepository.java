package com.example.blog.profile.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    /** Registra a relação; repetir não duplica nem falha. */
    @Modifying
    @Query(value = "INSERT INTO follows (follower_id, followed_id) VALUES (:followerId, :followedId) "
            + "ON CONFLICT DO NOTHING", nativeQuery = true)
    void seguir(@Param("followerId") long followerId, @Param("followedId") long followedId);

    /** Remove a relação, se existir. */
    @Modifying
    @Query(value = "DELETE FROM follows WHERE follower_id = :followerId AND followed_id = :followedId",
            nativeQuery = true)
    void deixarDeSeguir(@Param("followerId") long followerId, @Param("followedId") long followedId);
}
