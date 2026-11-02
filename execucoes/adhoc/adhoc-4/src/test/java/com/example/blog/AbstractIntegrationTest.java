package com.example.blog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base dos testes de integração: aplicação completa com PostgreSQL descartável (Testcontainers),
 * migrations aplicadas pelo Flyway e uma chave JWT própria dos testes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    /** Chave fictícia, válida apenas nos testes (mais de 32 bytes). */
    private static final String TEST_JWT_SECRET = Base64.getEncoder()
            .encodeToString("chave-de-teste-ficticia-somente-para-testes-0123456789".getBytes(StandardCharsets.UTF_8));

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM follows");
        jdbcTemplate.update("DELETE FROM users");
    }

    /** Cadastra um usuário fictício pela API e devolve o token recebido. */
    protected String registerUser(String username, String email, String password) throws Exception {
        String body = """
                {"user": {"username": "%s", "email": "%s", "password": "%s"}}
                """.formatted(username, email, password);
        String response = mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.user.token");
    }

    protected static String tokenHeader(String token) {
        return "Token " + token;
    }
}
