package com.example.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Aplica todas as migrations em banco vazio. A subida do contexto já valida o mapeamento JPA
 * ({@code ddl-auto: validate}); aqui conferimos também o histórico do Flyway.
 */
class SchemaMigrationIT extends AbstractIntegrationTest {

    @Test
    void deveAplicarTodasAsMigrationsComSucesso() {
        List<Boolean> results = jdbcTemplate.queryForList(
                "SELECT success FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank",
                Boolean.class);

        assertThat(results).isNotEmpty().containsOnly(true);
    }

    @Test
    void deveImpedirQueUmUsuarioSigaASiMesmoNoBanco() {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO users (username, email, password_hash) VALUES ('ana', 'ana@exemplo.test', 'x')
                RETURNING id
                """, Long.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO follows (follower_id, followed_id) VALUES (?, ?)", id, id))
                .hasMessageContaining("ck_follows_not_self");
    }
}
