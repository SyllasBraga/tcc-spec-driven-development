package com.example.blog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Aplica todas as migrations em banco vazio e valida o mapeamento JPA ({@code ddl-auto: validate}). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MigracoesIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveAplicarTodasAsMigrationsComSucesso() {
        List<String> versoes = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);
        Integer falhas = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE NOT success", Integer.class);

        assertThat(versoes).containsExactly("1", "2");
        assertThat(falhas).isZero();
    }
}
