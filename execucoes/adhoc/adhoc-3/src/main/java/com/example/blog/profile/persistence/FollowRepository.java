package com.example.blog.profile.persistence;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends Repository<Follow, FollowId> {

    boolean existsById(FollowId id);

    /** Registra a relação; não faz nada se ela já existir (seguro sob concorrência). */
    @Modifying
    @Query(value = """
            INSERT INTO public.user_follow (follower_id, followed_id)
            VALUES (:followerId, :followedId)
            ON CONFLICT ON CONSTRAINT pk_user_follow DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(@Param("followerId") long followerId, @Param("followedId") long followedId);

    @Modifying
    @Query("delete from Follow f where f.id.followerId = :followerId and f.id.followedId = :followedId")
    void delete(@Param("followerId") long followerId, @Param("followedId") long followedId);
}
