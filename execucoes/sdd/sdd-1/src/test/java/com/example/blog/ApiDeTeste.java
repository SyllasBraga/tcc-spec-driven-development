package com.example.blog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

/**
 * Atalhos dos testes de integração para criar usuários fictícios e montar requisições.
 */
public final class ApiDeTeste {

    public static final String SENHA = "senha-ficticia";

    private final MockMvc mockMvc;

    public ApiDeTeste(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /** Gera um username único, para os testes não dependerem de ordem nem de dados existentes. */
    public static String usernameUnico(String prefixo) {
        return prefixo + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String emailDe(String username) {
        return username + "@exemplo.test";
    }

    public static String cadastro(String username, String email, String senha) {
        return """
                {"user": {"username": "%s", "email": "%s", "password": "%s"}}
                """.formatted(username, email, senha);
    }

    public static String login(String email, String senha) {
        return """
                {"user": {"email": "%s", "password": "%s"}}
                """.formatted(email, senha);
    }

    public static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requisicao, String corpo) {
        return requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    public static String autorizacao(String token) {
        return "Token " + token;
    }

    public ResultActions registrar(String username, String email, String senha) throws Exception {
        return mockMvc.perform(json(post("/api/users"), cadastro(username, email, senha)));
    }

    public ResultActions entrar(String email, String senha) throws Exception {
        return mockMvc.perform(json(post("/api/users/login"), login(email, senha)));
    }

    /** Cadastra um usuário com username único e devolve o token. */
    public String registrarETomarToken(String username) throws Exception {
        return token(registrar(username, emailDe(username), SENHA));
    }

    public static String token(ResultActions resultado) throws Exception {
        String corpo = resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(corpo, "$.user.token");
    }
}
