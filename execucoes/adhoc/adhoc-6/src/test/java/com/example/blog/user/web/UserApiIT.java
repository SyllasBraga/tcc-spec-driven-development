package com.example.blog.user.web;

import static com.example.blog.user.web.ApiTestClient.json;
import static com.example.blog.user.web.ApiTestClient.withToken;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.blog.TestcontainersConfiguration;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class UserApiIT {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ApiTestClient api;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM user_follow");
        jdbcTemplate.update("DELETE FROM user_account");
        api = new ApiTestClient(mockMvc, jsonMapper);
    }

    @Test
    void registerReturnsCreatedUserWithToken() throws Exception {
        mockMvc.perform(json(post("/api/users"), """
                {"user": {"username": "Jacob", "email": "Jake@Jake.jake", "password": "jakejake"}}
                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Content-Type", JSON_UTF8))
                .andExpect(header().string("Location", "/api/profiles/Jacob"))
                .andExpect(jsonPath("$.user.username").value("Jacob"))
                .andExpect(jsonPath("$.user.email").value("jake@jake.jake"))
                .andExpect(jsonPath("$.user.token").value(not(emptyString())))
                .andExpect(jsonPath("$.user.bio").value(nullValue()))
                .andExpect(jsonPath("$.user.image").value(nullValue()))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void registerWithMissingFieldsReturns422() throws Exception {
        mockMvc.perform(json(post("/api/users"), """
                {"user": {"username": "", "email": "not-an-email"}}
                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(header().string("Content-Type", JSON_UTF8))
                .andExpect(jsonPath("$.errors.username[0]").value("can't be blank"))
                .andExpect(jsonPath("$.errors.email[0]").value("is invalid"))
                .andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
    }

    @Test
    void registerWithoutUserObjectReturns422() throws Exception {
        mockMvc.perform(json(post("/api/users"), "{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.user[0]").value("can't be blank"));
    }

    @Test
    void registerWithMalformedJsonReturns422() throws Exception {
        mockMvc.perform(json(post("/api/users"), "{\"user\":"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.body[0]").value("is invalid"));
    }

    @Test
    void registerWithTakenUsernameAndEmailReturns409() throws Exception {
        api.register("jake");

        mockMvc.perform(json(post("/api/users"), """
                {"user": {"username": "jake", "email": "JAKE@example.com", "password": "password123"}}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.username[0]").value("has already been taken"))
                .andExpect(jsonPath("$.errors.email[0]").value("has already been taken"));
    }

    @Test
    void loginReturnsUserWithToken() throws Exception {
        api.register("jake");

        mockMvc.perform(json(post("/api/users/login"), """
                {"user": {"email": "jake@example.com", "password": "password123"}}
                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", JSON_UTF8))
                .andExpect(jsonPath("$.user.username").value("jake"))
                .andExpect(jsonPath("$.user.token").value(not(emptyString())));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        api.register("jake");

        mockMvc.perform(json(post("/api/users/login"), """
                {"user": {"email": "jake@example.com", "password": "wrong-password"}}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.credentials[0]").value("invalid"));
    }

    @Test
    void loginWithMissingPasswordReturns422() throws Exception {
        mockMvc.perform(json(post("/api/users/login"), """
                {"user": {"email": "jake@example.com"}}
                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
    }

    @Test
    void getCurrentUserReturnsUserAndSameToken() throws Exception {
        String token = api.register("jake");

        mockMvc.perform(withToken(get("/api/user"), token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", JSON_UTF8))
                .andExpect(jsonPath("$.user.username").value("jake"))
                .andExpect(jsonPath("$.user.email").value("jake@example.com"))
                .andExpect(jsonPath("$.user.token").value(token));
    }

    @Test
    void getCurrentUserWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", JSON_UTF8))
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    @Test
    void getCurrentUserWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(withToken(get("/api/user"), "not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void updateUserChangesFieldsAndAllowsLoginWithNewPassword() throws Exception {
        String token = api.register("jake");

        mockMvc.perform(withToken(json(put("/api/user"), """
                {"user": {"email": "new@example.com", "username": "jacob", "password": "new-password",
                          "bio": "I like to skateboard", "image": "https://example.com/jake.png"}}
                """), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("new@example.com"))
                .andExpect(jsonPath("$.user.username").value("jacob"))
                .andExpect(jsonPath("$.user.bio").value("I like to skateboard"))
                .andExpect(jsonPath("$.user.image").value("https://example.com/jake.png"))
                .andExpect(jsonPath("$.user.token").value(token));

        mockMvc.perform(json(post("/api/users/login"), """
                {"user": {"email": "new@example.com", "password": "new-password"}}
                """))
                .andExpect(status().isOk());
    }

    @Test
    void updateUserWithNullBioClearsItAndKeepsOtherFields() throws Exception {
        String token = api.register("jake");
        mockMvc.perform(withToken(json(put("/api/user"), """
                {"user": {"bio": "some bio", "image": "https://example.com/jake.png"}}
                """), token))
                .andExpect(status().isOk());

        mockMvc.perform(withToken(json(put("/api/user"), """
                {"user": {"bio": null}}
                """), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value(nullValue()))
                .andExpect(jsonPath("$.user.image").value("https://example.com/jake.png"))
                .andExpect(jsonPath("$.user.username").value("jake"));
    }

    @Test
    void updateUserWithEmailOfAnotherUserReturns409() throws Exception {
        api.register("other");
        String token = api.register("jake");

        mockMvc.perform(withToken(json(put("/api/user"), """
                {"user": {"email": "other@example.com"}}
                """), token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.email[0]").value("has already been taken"));
    }

    @Test
    void updateUserWithBlankUsernameReturns422() throws Exception {
        String token = api.register("jake");

        mockMvc.perform(withToken(json(put("/api/user"), """
                {"user": {"username": "  "}}
                """), token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.username[0]").value("can't be blank"));
    }

    @Test
    void updateUserWithNullEmailReturns422() throws Exception {
        String token = api.register("jake");

        mockMvc.perform(withToken(json(put("/api/user"), """
                {"user": {"email": null, "bio": "I like to skateboard"}}
                """), token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("can't be blank"));

        mockMvc.perform(withToken(get("/api/user"), token))
                .andExpect(jsonPath("$.user.bio").value(nullValue()));
    }

    @Test
    void updateUserWithNumberInsteadOfStringReturns422() throws Exception {
        String token = api.register("jake");

        mockMvc.perform(withToken(json(put("/api/user"), """
                {"user": {"bio": 123}}
                """), token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.body[0]").value("is invalid"));
    }

    @Test
    void updateUserWithoutFieldsReturns422() throws Exception {
        String token = api.register("jake");

        mockMvc.perform(withToken(json(put("/api/user"), """
                {"user": {}}
                """), token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.user[0]").value("can't be empty"));
    }

    @Test
    void updateUserWithoutTokenReturns401() throws Exception {
        mockMvc.perform(json(put("/api/user"), """
                {"user": {"bio": "x"}}
                """))
                .andExpect(status().isUnauthorized());
    }
}
