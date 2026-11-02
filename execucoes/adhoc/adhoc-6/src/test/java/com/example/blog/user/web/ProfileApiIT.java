package com.example.blog.user.web;

import static com.example.blog.user.web.ApiTestClient.withToken;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class ProfileApiIT {

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
    void getProfileWithoutTokenReturnsNotFollowing() throws Exception {
        api.register("celeb");

        mockMvc.perform(get("/api/profiles/celeb"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", JSON_UTF8))
                .andExpect(jsonPath("$.profile.username").value("celeb"))
                .andExpect(jsonPath("$.profile.bio").value(nullValue()))
                .andExpect(jsonPath("$.profile.image").value(nullValue()))
                .andExpect(jsonPath("$.profile.following").value(false))
                .andExpect(jsonPath("$.profile.email").doesNotExist());
    }

    @Test
    void getUnknownProfileReturns404() throws Exception {
        mockMvc.perform(get("/api/profiles/nobody"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", JSON_UTF8))
                .andExpect(jsonPath("$.errors.profile[0]").value("not found"));
    }

    @Test
    void followAndUnfollowUpdateTheFollowingFlag() throws Exception {
        api.register("celeb");
        String fanToken = api.register("fan");

        mockMvc.perform(withToken(post("/api/profiles/celeb/follow"), fanToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", JSON_UTF8))
                .andExpect(jsonPath("$.profile.username").value("celeb"))
                .andExpect(jsonPath("$.profile.following").value(true));
        mockMvc.perform(withToken(get("/api/profiles/celeb"), fanToken))
                .andExpect(jsonPath("$.profile.following").value(true));
        mockMvc.perform(get("/api/profiles/celeb"))
                .andExpect(jsonPath("$.profile.following").value(false));

        mockMvc.perform(withToken(delete("/api/profiles/celeb/follow"), fanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));
        mockMvc.perform(withToken(get("/api/profiles/celeb"), fanToken))
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void followTwiceIsIdempotent() throws Exception {
        api.register("celeb");
        String fanToken = api.register("fan");

        mockMvc.perform(withToken(post("/api/profiles/celeb/follow"), fanToken)).andExpect(status().isOk());
        mockMvc.perform(withToken(post("/api/profiles/celeb/follow"), fanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(true));
    }

    @Test
    void unfollowWhenNotFollowingReturnsNotFollowing() throws Exception {
        api.register("celeb");
        String fanToken = api.register("fan");

        mockMvc.perform(withToken(delete("/api/profiles/celeb/follow"), fanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void followWithoutTokenReturns401() throws Exception {
        api.register("celeb");

        mockMvc.perform(post("/api/profiles/celeb/follow"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
        mockMvc.perform(delete("/api/profiles/celeb/follow"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void followUnknownProfileReturns404() throws Exception {
        String fanToken = api.register("fan");

        mockMvc.perform(withToken(post("/api/profiles/nobody/follow"), fanToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.profile[0]").value("not found"));
    }

    @Test
    void followYourselfReturns422() throws Exception {
        String token = api.register("narcissus");

        mockMvc.perform(withToken(post("/api/profiles/narcissus/follow"), token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.profile[0]").value("can't follow yourself"));
    }
}
