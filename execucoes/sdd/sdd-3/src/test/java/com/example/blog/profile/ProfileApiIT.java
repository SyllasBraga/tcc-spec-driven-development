package com.example.blog.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.blog.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;

/**
 * Cenários de aceitação de US3 (perfil público) e US4 (seguir e deixar de seguir), ponta a ponta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProfileApiIT {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenJake;

    @BeforeEach
    void preparar() throws Exception {
        jdbcTemplate.update("DELETE FROM follows");
        jdbcTemplate.update("DELETE FROM users");
        tokenJake = cadastrar("jake", "jake@jake.jake");
        cadastrar("celeb", "celeb@x.com");
    }

    // ---------- US3 ----------

    @Test
    void us3Cenario1_deveBuscarPerfilSemAutenticacao() throws Exception {
        mockMvc.perform(com(tokenJake, put("/api/user").contentType(MediaType.APPLICATION_JSON).content("""
                        {"user":{"bio":"I work at statefarm","image":"https://x.com/jake.jpg"}}
                        """)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/profiles/jake"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"profile":{"username":"jake","bio":"I work at statefarm",
                        "image":"https://x.com/jake.jpg","following":false}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void us3Cenario2_deveResponder404ParaPerfilInexistente() throws Exception {
        mockMvc.perform(get("/api/profiles/ninguem"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"errors":{"profile":["not found"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveDiferenciarMaiusculasNoUsername() throws Exception {
        mockMvc.perform(get("/api/profiles/JAKE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void us3Cenario3_deveIndicarFollowingQuandoSegue() throws Exception {
        jdbcTemplate.update("INSERT INTO follows (follower_id, followed_id) VALUES (?, ?)", id("jake"), id("celeb"));

        mockMvc.perform(com(tokenJake, get("/api/profiles/celeb")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(true));
    }

    @Test
    void us3Cenario4_deveResponder401ComTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/profiles/jake").header("Authorization", "Token abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("""
                        {"errors":{"token":["is invalid"]}}
                        """, JsonCompareMode.STRICT));
    }

    // ---------- US4 ----------

    @Test
    void us4Cenarios1e2_deveSeguirDeFormaIdempotente() throws Exception {
        String seguindo = """
                {"profile":{"username":"celeb","bio":null,"image":null,"following":true}}
                """;

        seguir().andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json(seguindo, JsonCompareMode.STRICT));
        seguir().andExpect(status().isOk())
                .andExpect(content().json(seguindo, JsonCompareMode.STRICT));

        Integer relacoes = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM follows WHERE follower_id = ? AND followed_id = ?", Integer.class,
                id("jake"), id("celeb"));
        assertThat(relacoes).isEqualTo(1);
        mockMvc.perform(com(tokenJake, get("/api/profiles/celeb")))
                .andExpect(jsonPath("$.profile.following").value(true));
    }

    @Test
    void us4Cenario3_deveDeixarDeSeguir() throws Exception {
        seguir().andExpect(status().isOk());

        deixarDeSeguir()
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"profile":{"username":"celeb","bio":null,"image":null,"following":false}}
                        """, JsonCompareMode.STRICT));
        mockMvc.perform(com(tokenJake, get("/api/profiles/celeb")))
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void us4Cenario4_deveDeixarDeSeguirQuemNaoSegue() throws Exception {
        deixarDeSeguir().andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));
        deixarDeSeguir().andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void us4Cenario5_deveResponder401SemToken() throws Exception {
        String erro = """
                {"errors":{"token":["is missing"]}}
                """;
        mockMvc.perform(post("/api/profiles/celeb/follow"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json(erro, JsonCompareMode.STRICT));
        mockMvc.perform(delete("/api/profiles/celeb/follow"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json(erro, JsonCompareMode.STRICT));
    }

    @Test
    void us4Cenario6_deveResponder404AoSeguirInexistente() throws Exception {
        mockMvc.perform(com(tokenJake, post("/api/profiles/ninguem/follow")))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"errors":{"profile":["not found"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveRecusarSeguirASiMesmo() throws Exception {
        mockMvc.perform(com(tokenJake, post("/api/profiles/jake/follow")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"profile":["cannot follow yourself"]}}
                        """, JsonCompareMode.STRICT));
    }

    private String cadastrar(String username, String email) throws Exception {
        String corpo = mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"user":{"username":"%s","email":"%s","password":"senha123"}}
                        """.formatted(username, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.user.token");
    }

    private Long id(String username) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    private ResultActions seguir() throws Exception {
        return mockMvc.perform(com(tokenJake, post("/api/profiles/celeb/follow")));
    }

    private ResultActions deixarDeSeguir() throws Exception {
        return mockMvc.perform(com(tokenJake, delete("/api/profiles/celeb/follow")));
    }

    private static MockHttpServletRequestBuilder com(String token, MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Token " + token);
    }
}
