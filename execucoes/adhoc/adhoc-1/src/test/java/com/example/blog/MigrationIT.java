package com.example.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Aplica todas as migrations em um banco vazio e sobe o contexto, o que valida o mapeamento JPA
 * ({@code ddl-auto: validate}) contra o esquema.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class MigrationIT {

    @Autowired
    private Flyway flyway;

    @Test
    void deveAplicarTodasAsMigrationsEmBancoVazio() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied())
                .isNotEmpty()
                .extracting(MigrationInfo::getState)
                .containsOnly(MigrationState.SUCCESS);
    }
}
