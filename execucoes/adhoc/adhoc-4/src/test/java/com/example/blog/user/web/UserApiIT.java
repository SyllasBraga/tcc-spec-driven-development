package com.example.blog.user.web;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;

import com.example.blog.AbstractIntegrationTest;

class UserApiIT extends AbstractIntegrationTest {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";

    @Test
    void deveCadastrarUsuarioERetornarTokenComBioEImageNulos() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"username": "Jacob", "email": "Jake@Jake.jake", "password": "jakejake"}}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, JSON_UTF8))
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/profiles/Jacob"))
                .andExpect(content().json("""
                        {"user": {"email": "jake@jake.jake", "username": "Jacob", "bio": null, "image": null}}
                        """))
                .andExpect(jsonPath("$.user.token").value(not(emptyString())))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void deveRetornar422QuandoCamposObrigatoriosFaltamNoCadastro() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"username": "", "email": "nao-e-email"}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors": {"username": ["can't be blank"], "email": ["is invalid"],
                                    "password": ["can't be blank"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveRetornar422QuandoEnvelopeUserFalta() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.user[0]").value("can't be blank"));
    }

    @Test
    void deveRetornar422QuandoJsonEhMalformado() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{\"user\": "))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.body[0]").value("is invalid"));
    }

    @Test
    void deveRetornar409QuandoUsernameOuEmailJaExistem() throws Exception {
        registerUser("jake", "jake@jake.jake", "jakejake");

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"username": "jake", "email": "outro@jake.jake", "password": "jakejake"}}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.username[0]").value("has already been taken"));
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"username": "outro", "email": "JAKE@jake.jake", "password": "jakejake"}}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.email[0]").value("has already been taken"));
    }

    @Test
    void deveFazerLoginComCredenciaisValidas() throws Exception {
        registerUser("jake", "jake@jake.jake", "jakejake");

        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"email": "jake@jake.jake", "password": "jakejake"}}
                        """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, JSON_UTF8))
                .andExpect(jsonPath("$.user.username").value("jake"))
                .andExpect(jsonPath("$.user.token").value(not(emptyString())));
    }

    @Test
    void deveRetornar401QuandoSenhaEstaErrada() throws Exception {
        registerUser("jake", "jake@jake.jake", "jakejake");

        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"email": "jake@jake.jake", "password": "errada"}}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.credentials[0]").value("invalid"));
    }

    @Test
    void deveRetornar422QuandoLoginSemSenha() throws Exception {
        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"email": "jake@jake.jake"}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
    }

    @Test
    void deveRetornarUsuarioAtualComOMesmoToken() throws Exception {
        String token = registerUser("jake", "jake@jake.jake", "jakejake");

        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, tokenHeader(token)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, JSON_UTF8))
                .andExpect(content().json("""
                        {"user": {"email": "jake@jake.jake", "token": "%s", "username": "jake",
                                  "bio": null, "image": null}}
                        """.formatted(token)));
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, JSON_UTF8))
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    @Test
    void deveRetornar401ComTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Token abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void deveAtualizarUsuarioEPermitirLoginComNovaSenha() throws Exception {
        String token = registerUser("jake", "jake@jake.jake", "jakejake");

        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION, tokenHeader(token))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"email": "novo@jake.jake", "bio": "I like to skateboard",
                                  "image": "https://i.stack.imgur.com/xHWG8.jpg", "password": "novasenha"}}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"user": {"email": "novo@jake.jake", "username": "jake", "bio": "I like to skateboard",
                                  "image": "https://i.stack.imgur.com/xHWG8.jpg", "token": "%s"}}
                        """.formatted(token)));

        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"email": "novo@jake.jake", "password": "novasenha"}}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void deveManterCamposNaoInformadosERemoverBioComTextoVazio() throws Exception {
        String token = registerUser("jake", "jake@jake.jake", "jakejake");
        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION, tokenHeader(token))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"bio": "bio inicial", "image": "https://exemplo.test/a.png"}}
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION, tokenHeader(token))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"bio": ""}}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"user": {"email": "jake@jake.jake", "username": "jake", "bio": null,
                                  "image": "https://exemplo.test/a.png"}}
                        """));
    }

    @Test
    void deveRemoverBioEImageQuandoInformadosComoNull() throws Exception {
        String token = registerUser("jake", "jake@jake.jake", "jakejake");
        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION, tokenHeader(token))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"bio": "bio inicial", "image": "https://exemplo.test/a.png"}}
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION, tokenHeader(token))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"bio": null, "image": null}}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value(nullValue()))
                .andExpect(jsonPath("$.user.image").value(nullValue()));

        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, tokenHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value(nullValue()))
                .andExpect(jsonPath("$.user.image").value(nullValue()));
    }

    @Test
    void deveRetornar422AoAtualizarEmailParaNull() throws Exception {
        String token = registerUser("jake", "jake@jake.jake", "jakejake");

        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION, tokenHeader(token))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"email": null}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("can't be blank"));
    }

    @Test
    void deveRetornar422AoAtualizarComCampoDeTipoErrado() throws Exception {
        String token = registerUser("jake", "jake@jake.jake", "jakejake");

        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION, tokenHeader(token))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"bio": 123}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.bio[0]").value("is invalid"));
    }

    @Test
    void deveRetornar422AoAtualizarParaEmailDeOutroUsuario() throws Exception {
        registerUser("outro", "outro@jake.jake", "jakejake");
        String token = registerUser("jake", "jake@jake.jake", "jakejake");

        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION, tokenHeader(token))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"email": "outro@jake.jake"}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("has already been taken"));
    }

    @Test
    void deveRetornar422AoAtualizarSemNenhumCampo() throws Exception {
        String token = registerUser("jake", "jake@jake.jake", "jakejake");

        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION, tokenHeader(token))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.user[0]").value("must have at least one field"));
    }

    @Test
    void deveRetornar401AoAtualizarSemToken() throws Exception {
        mockMvc.perform(put("/api/user").contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"bio": "x"}}
                        """))
                .andExpect(status().isUnauthorized());
    }
}
