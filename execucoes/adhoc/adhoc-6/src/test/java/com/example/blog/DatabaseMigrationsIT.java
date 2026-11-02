package com.example.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Starts the application against an empty PostgreSQL: every migration must apply and the JPA mapping must
 * match the resulting schema ({@code ddl-auto: validate}), otherwise the context fails to start.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class DatabaseMigrationsIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allMigrationsAreAppliedSuccessfully() {
        Integer failed = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE NOT success", Integer.class);
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL", Integer.class);

        assertThat(failed).isZero();
        assertThat(applied).isGreaterThanOrEqualTo(2);
    }
}
