package com.example.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Aplica todas as migrations em banco vazio e valida o mapeamento JPA ({@code ddl-auto: validate}):
 * se uma migration quebrar ou uma entidade divergir do esquema, o contexto não sobe.
 */
@TesteDeIntegracao
class MigrationsIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveAplicarTodasAsMigrationsComSucesso() {
        List<String> versoesAplicadas = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

        assertThat(versoesAplicadas).containsExactly("1", "2");
    }

    @Test
    void deveImpedirNoBancoQueUsuarioSigaASiMesmo() {
        Long id = inserirUsuario();

        assertThatThrownBy(() -> inserirSeguimento(id, id))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_seguidor_distintos");
    }

    @Test
    void deveImpedirNoBancoSeguimentoRepetido() {
        Long seguidor = inserirUsuario();
        Long seguido = inserirUsuario();
        inserirSeguimento(seguidor, seguido);

        assertThatThrownBy(() -> inserirSeguimento(seguidor, seguido))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("pk_seguidor");
    }

    private Long inserirUsuario() {
        String sufixo = UUID.randomUUID().toString();
        return jdbcTemplate.queryForObject(
                "INSERT INTO usuario (email, username, senha_hash) VALUES (?, ?, ?) RETURNING id",
                Long.class, "migracao-" + sufixo + "@exemplo.test", "migracao-" + sufixo, "{noop}ficticio");
    }

    private void inserirSeguimento(Long seguidorId, Long seguidoId) {
        jdbcTemplate.update("INSERT INTO seguidor (seguidor_id, seguido_id) VALUES (?, ?)", seguidorId, seguidoId);
    }
}
