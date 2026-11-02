package com.example.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sobe a aplicação sobre um PostgreSQL vazio: todas as migrations precisam ser aplicadas com sucesso e o
 * mapeamento JPA precisa passar no {@code ddl-auto: validate}.
 */
@ApiIntegrationTest
class MigrationsIT {

    @Autowired
    private Flyway flyway;

    @Test
    void deveAplicarTodasAsMigrationsSemPendencias() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied())
                .isNotEmpty()
                .allMatch(MigrationInfo::isApplied)
                .noneMatch(info -> info.getState().isFailed());
    }
}
