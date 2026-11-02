package com.example.blog.perfil.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeguimentoRepository extends JpaRepository<Seguimento, SeguimentoId> {

    /**
     * Grava a relação de forma idempotente: se o par já existir (inclusive por requisições concorrentes),
     * nada é alterado.
     *
     * @return 1 se a relação foi criada, 0 se já existia
     */
    @Modifying
    @Query(value = """
            insert into public.seguimento (seguidor_id, seguido_id, criado_em)
            values (:seguidorId, :seguidoId, :criadoEm)
            on conflict on constraint pk_seguimento do nothing
            """, nativeQuery = true)
    int inserirSeNaoExistir(@Param("seguidorId") Long seguidorId, @Param("seguidoId") Long seguidoId,
            @Param("criadoEm") Instant criadoEm);
}
