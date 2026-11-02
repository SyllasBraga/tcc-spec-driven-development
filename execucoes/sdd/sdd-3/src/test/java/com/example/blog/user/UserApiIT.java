package com.example.blog.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.blog.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Cenários de aceitação de US1 (cadastro e login) e US2 (usuário autenticado), ponta a ponta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserApiIT {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";
    private static final String JAKE = """
            {"user":{"username":"jake","email":"jake@jake.jake","password":"jakejake"}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limparBanco() {
        jdbcTemplate.update("DELETE FROM follows");
        jdbcTemplate.update("DELETE FROM users");
    }

    // ---------- US1 ----------

    @Test
    void us1Cenario1_deveCadastrarComStatus201() throws Exception {
        String corpo = postJson("/api/users", JAKE)
                .andExpect(status().isCreated())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"user":{"email":"jake@jake.jake","username":"jake","bio":null,"image":null}}
                        """))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(corpo, "$.user.token");
        assertThat(token.split("\\.")).hasSize(3);
        assertThat(corpo).doesNotContain("password").doesNotContain("bcrypt");
    }

    @Test
    void us1Cenario2_deveRecusarUsernameRepetidoCom409() throws Exception {
        cadastrar("jake", "jake@jake.jake", "jakejake");

        postJson("/api/users", """
                {"user":{"username":"jake","email":"outro@jake.jake","password":"jakejake"}}
                """)
                .andExpect(status().isConflict())
                .andExpect(content().json("""
                        {"errors":{"username":["has already been taken"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveRecusarUsernameEEmailRepetidosComAsDuasChaves() throws Exception {
        cadastrar("jake", "jake@jake.jake", "jakejake");

        postJson("/api/users", """
                {"user":{"username":"jake","email":"JAKE@jake.jake","password":"jakejake"}}
                """)
                .andExpect(status().isConflict())
                .andExpect(content().json("""
                        {"errors":{"username":["has already been taken"],"email":["has already been taken"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void us1Cenario3_deveRecusarCadastroSemEmail() throws Exception {
        postJson("/api/users", """
                {"user":{"username":"jake","password":"jakejake"}}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"email":["can't be blank"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void us1Cenario4_deveEntrarComCredenciaisValidas() throws Exception {
        cadastrar("jake", "jake@jake.jake", "jakejake");

        postJson("/api/users/login", """
                {"user":{"email":"jake@jake.jake","password":"jakejake"}}
                """)
                .andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.user.username").value("jake"))
                .andExpect(jsonPath("$.user.token").isNotEmpty());
    }

    @Test
    void deveEntrarComEmailEmMaiusculas() throws Exception {
        cadastrar("jake", "jake@jake.jake", "jakejake");

        postJson("/api/users/login", """
                {"user":{"email":"JAKE@Jake.jake","password":"jakejake"}}
                """)
                .andExpect(status().isOk());
    }

    @Test
    void us1Cenario5_deveRecusarSenhaErradaOuEmailInexistente() throws Exception {
        cadastrar("jake", "jake@jake.jake", "jakejake");
        String erro = """
                {"errors":{"credentials":["invalid"]}}
                """;

        postJson("/api/users/login", """
                {"user":{"email":"jake@jake.jake","password":"errada"}}
                """)
                .andExpect(status().isUnauthorized())
                .andExpect(content().json(erro, JsonCompareMode.STRICT));
        postJson("/api/users/login", """
                {"user":{"email":"ninguem@jake.jake","password":"jakejake"}}
                """)
                .andExpect(status().isUnauthorized())
                .andExpect(content().json(erro, JsonCompareMode.STRICT));
    }

    // ---------- US2 ----------

    @Test
    void us2Cenario1_deveBuscarUsuarioAutenticado() throws Exception {
        String token = cadastrar("jake", "jake@jake.jake", "jakejake");

        mockMvc.perform(get("/api/user").header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.user.email").value("jake@jake.jake"))
                .andExpect(jsonPath("$.user.username").value("jake"))
                .andExpect(jsonPath("$.user.token").isNotEmpty());
    }

    @Test
    void us2Cenario2_deveResponder401SemCabecalho() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"errors":{"token":["is missing"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveResponder401ComTokenAssinadoComOutraChave() throws Exception {
        cadastrar("jake", "jake@jake.jake", "jakejake");
        String falso = tokenAssinadoComOutraChave();

        mockMvc.perform(get("/api/user").header("Authorization", "Token " + falso))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("""
                        {"errors":{"token":["is invalid"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveResponder401ComEsquemaBearer() throws Exception {
        String token = cadastrar("jake", "jake@jake.jake", "jakejake");

        mockMvc.perform(get("/api/user").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("""
                        {"errors":{"token":["is invalid"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveResponder401ComTokenDeUsuarioApagado() throws Exception {
        String token = cadastrar("jake", "jake@jake.jake", "jakejake");
        jdbcTemplate.update("DELETE FROM users WHERE username = ?", "jake");

        mockMvc.perform(get("/api/user").header("Authorization", "Token " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("""
                        {"errors":{"token":["is invalid"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void us2Cenario3_deveAtualizarBioEImagemMantendoDemais() throws Exception {
        String token = cadastrar("jake", "jake@jake.jake", "jakejake");

        putUser(token, """
                {"user":{"bio":"I like to skateboard","image":"https://i.stack.imgur.com/xHWG8.jpg"}}
                """)
                .andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"user":{"email":"jake@jake.jake","username":"jake","bio":"I like to skateboard",
                        "image":"https://i.stack.imgur.com/xHWG8.jpg"}}
                        """))
                .andExpect(jsonPath("$.user.token").isNotEmpty());
    }

    @Test
    void deveLimparBioComNulo() throws Exception {
        String token = cadastrar("jake", "jake@jake.jake", "jakejake");
        putUser(token, """
                {"user":{"bio":"algo"}}
                """).andExpect(status().isOk());

        putUser(token, """
                {"user":{"bio":null}}
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"user":{"bio":null}}
                        """));
    }

    @Test
    void deveRecusarAtualizacaoVazia() throws Exception {
        String token = cadastrar("jake", "jake@jake.jake", "jakejake");

        putUser(token, """
                {"user":{}}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors").isMap());
    }

    @Test
    void us2Cenario4_deveAceitarSomenteANovaSenha() throws Exception {
        String token = cadastrar("jake", "jake@jake.jake", "jakejake");

        putUser(token, """
                {"user":{"password":"novasenha"}}
                """).andExpect(status().isOk());

        postJson("/api/users/login", """
                {"user":{"email":"jake@jake.jake","password":"novasenha"}}
                """).andExpect(status().isOk());
        postJson("/api/users/login", """
                {"user":{"email":"jake@jake.jake","password":"jakejake"}}
                """).andExpect(status().isUnauthorized());
    }

    @Test
    void us2Cenario5_deveRecusarEmailDeOutroUsuarioCom422() throws Exception {
        cadastrar("outro", "outro@x.com", "senha123");
        String token = cadastrar("jake", "jake@jake.jake", "jakejake");

        putUser(token, """
                {"user":{"email":"outro@x.com"}}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"email":["has already been taken"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveRecusarUsernameDeOutroUsuarioCom422() throws Exception {
        cadastrar("outro", "outro@x.com", "senha123");
        String token = cadastrar("jake", "jake@jake.jake", "jakejake");

        putUser(token, """
                {"user":{"username":"outro"}}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"username":["has already been taken"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void tokenAntigoContinuaValidoAposTrocarUsername() throws Exception {
        String token = cadastrar("jake", "jake@jake.jake", "jakejake");

        putUser(token, """
                {"user":{"username":"jacob"}}
                """).andExpect(status().isOk());

        mockMvc.perform(get("/api/user").header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("jacob"));
    }

    private String cadastrar(String username, String email, String senha) throws Exception {
        String corpo = postJson("/api/users", """
                {"user":{"username":"%s","email":"%s","password":"%s"}}
                """.formatted(username, email, senha))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.user.token");
    }

    private ResultActions postJson(String caminho, String corpo) throws Exception {
        return mockMvc.perform(post(caminho).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions putUser(String token, String corpo) throws Exception {
        return mockMvc.perform(put("/api/user")
                .header("Authorization", "Token " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private String tokenAssinadoComOutraChave() {
        byte[] outraChave = "outra-chave-ficticia-de-32-bytes".getBytes(StandardCharsets.US_ASCII);
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(outraChave, "HmacSHA256")));
        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Long id = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'jake'", Long.class);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("conduit-blog-test")
                .subject(String.valueOf(id))
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(3600))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
