package com.example.blog.profile.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<FollowEntity, FollowId> {

    /**
     * Registra a relação; se ela já existir (inclusive por uma requisição concorrente), não faz nada.
     *
     * @return {@code 1} se a relação foi criada, {@code 0} se já existia
     */
    @Modifying
    @Query(value = """
            insert into public.follows (follower_id, followed_id, created_at)
            values (:followerId, :followedId, :criadoEm)
            on conflict on constraint pk_follows do nothing
            """, nativeQuery = true)
    int inserirSeAusente(@Param("followerId") Long followerId, @Param("followedId") Long followedId,
            @Param("criadoEm") Instant criadoEm);
}
