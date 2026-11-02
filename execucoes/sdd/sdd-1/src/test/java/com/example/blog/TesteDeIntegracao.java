package com.example.blog;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Configuração comum dos testes de integração: contexto completo, MockMvc com todos os filtros
 * (segurança e codificação), PostgreSQL 18.6 descartável via Testcontainers e perfil {@code test}.
 * Usar sempre esta anotação mantém um único contexto (e um único contêiner) em cache entre os testes.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public @interface TesteDeIntegracao {
}
