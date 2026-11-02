package com.example.blog;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.example.blog.shared.seguranca.SegredoJwtAleatorioConfiguration;

/** Banco PostgreSQL descartável (mesma versão do projeto) e segredo JWT aleatório para os testes de integração. */
@TestConfiguration(proxyBeanMethods = false)
@Import(SegredoJwtAleatorioConfiguration.class)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18.6"));
    }

}
