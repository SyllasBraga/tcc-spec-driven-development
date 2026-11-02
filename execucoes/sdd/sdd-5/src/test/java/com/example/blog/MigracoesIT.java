package com.example.blog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Sobe o contexto completo sobre um PostgreSQL 18.6 vazio: o Flyway aplica todas as migrations e o
 * Hibernate valida o mapeamento ({@code ddl-auto: validate}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, ApoioDeTeste.class})
@ActiveProfiles("test")
class MigracoesIT {

    private final JdbcTemplate jdbcTemplate;

    MigracoesIT(@Autowired JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void deveAplicarTodasAsMigrationsComSucesso() {
        List<String> aplicadas = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success and version is not null "
                        + "order by installed_rank",
                String.class);

        assertThat(aplicadas).containsExactly("1", "2");
    }
}
