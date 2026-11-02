package com.example.blog.user.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Helpers shared by the API integration tests.
 */
final class ApiTestClient {

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;

    ApiTestClient(MockMvc mockMvc, JsonMapper jsonMapper) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
    }

    /** Registers a user with fictitious data and returns its token. */
    String register(String username) throws Exception {
        String body = """
                {"user": {"username": "%s", "email": "%s@example.com", "password": "password123"}}
                """.formatted(username, username.toLowerCase());
        String response = mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = jsonMapper.readTree(response);
        return json.get("user").get("token").asString();
    }

    static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Token " + token);
    }

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
