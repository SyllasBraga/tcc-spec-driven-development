package com.example.blog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Aplica todas as migrations em banco vazio (Testcontainers) e confirma que o mapeamento JPA passa no
 * {@code ddl-auto: validate}, pois o contexto só sobe se a validação passar.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MigracoesIT {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void deveAplicarTodasAsMigrationsComSucesso() {
		List<Boolean> sucessos = jdbcTemplate.queryForList(
				"SELECT success FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank",
				Boolean.class);

		assertThat(sucessos).isNotEmpty().containsOnly(true);
	}

	@Test
	void deveCriarAsVersoesEsperadas() {
		List<String> versoes = jdbcTemplate.queryForList(
				"SELECT version FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank",
				String.class);

		assertThat(versoes).startsWith("1", "2");
	}

}
