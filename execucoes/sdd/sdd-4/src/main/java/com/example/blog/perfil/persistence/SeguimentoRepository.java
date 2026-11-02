package com.example.blog.perfil.persistence;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeguimentoRepository extends JpaRepository<Seguimento, SeguimentoId> {

    /**
     * Registra a relação, sem erro se ela já existir (idempotente mesmo com requisições simultâneas).
     *
     * @return 1 se a relação foi criada, 0 se já existia
     */
    @Modifying
    @Query(value = """
            INSERT INTO public.seguimento (seguidor_id, seguido_id, criado_em)
            VALUES (:seguidorId, :seguidoId, :criadoEm)
            ON CONFLICT ON CONSTRAINT pk_seguimento DO NOTHING
            """, nativeQuery = true)
    int seguir(@Param("seguidorId") long seguidorId, @Param("seguidoId") long seguidoId,
            @Param("criadoEm") Instant criadoEm);

    /**
     * Remove a relação, sem erro se ela não existir.
     *
     * @return quantidade de relações removidas (0 ou 1)
     */
    @Modifying
    @Query("DELETE FROM Seguimento s WHERE s.id.seguidorId = :seguidorId AND s.id.seguidoId = :seguidoId")
    int deixarDeSeguir(@Param("seguidorId") long seguidorId, @Param("seguidoId") long seguidoId);
}
