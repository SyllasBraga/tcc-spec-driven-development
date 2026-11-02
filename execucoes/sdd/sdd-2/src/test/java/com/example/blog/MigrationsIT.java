package com.example.blog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Sobe a aplicação num PostgreSQL vazio: o Flyway aplica todas as migrations e o Hibernate
 * valida o mapeamento JPA ({@code ddl-auto: validate}).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MigrationsIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @Test
    void deveAplicarTodasAsMigrationsEValidarOMapeamento() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");

        List<String> versoes = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success order by installed_rank", String.class);
        Integer falhas = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where not success", Integer.class);

        assertThat(versoes).containsExactly("1", "2");
        assertThat(falhas).isZero();
    }
}
