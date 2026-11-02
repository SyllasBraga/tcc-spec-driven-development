package com.example.blog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Sobe o contexto em banco vazio: o Flyway aplica todas as migrations e o Hibernate valida o mapeamento.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MigrationsIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveAplicarTodasAsMigrationsComSucesso() {
        List<String> versoes = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = true AND version IS NOT NULL "
                        + "ORDER BY installed_rank",
                String.class);

        assertThat(versoes).containsExactly("1", "2");
    }
}
