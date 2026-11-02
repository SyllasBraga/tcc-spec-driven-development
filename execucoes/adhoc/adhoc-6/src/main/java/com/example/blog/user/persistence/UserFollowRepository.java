package com.example.blog.user.persistence;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFollowRepository extends JpaRepository<UserFollow, UserFollowId> {

    /**
     * Records that {@code followerId} follows {@code followedId}. Idempotent, also under concurrent requests.
     *
     * @return 1 if the follow was created, 0 if it already existed
     */
    @Modifying
    @Query(value = """
            INSERT INTO user_follow (follower_id, followed_id, created_at)
            VALUES (:followerId, :followedId, :createdAt)
            ON CONFLICT ON CONSTRAINT pk_user_follow DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("followerId") long followerId, @Param("followedId") long followedId,
            @Param("createdAt") Instant createdAt);

    /**
     * Removes the follow, if present.
     *
     * @return number of removed rows (0 or 1)
     */
    @Modifying
    @Query("delete from UserFollow f where f.id.followerId = :followerId and f.id.followedId = :followedId")
    int deleteFollow(@Param("followerId") long followerId, @Param("followedId") long followedId);
}
