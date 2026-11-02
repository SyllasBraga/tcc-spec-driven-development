package com.example.blog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Cria usuários fictícios e únicos pela própria API, para que cada teste tenha seus dados. */
public final class TestUsers {

    public static final String PASSWORD = "senha-ficticia-123";

    private TestUsers() {
    }

    public static String uniqueUsername() {
        return "user" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /** Cadastra um usuário com o nome informado e devolve o token emitido. */
    public static String register(MockMvc mockMvc, String username) throws Exception {
        String body = """
                {"user": {"username": "%s", "email": "%s@example.test", "password": "%s"}}
                """.formatted(username, username, PASSWORD);
        String response = mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.user.token");
    }

    public static String authorization(String token) {
        return "Token " + token;
    }
}
