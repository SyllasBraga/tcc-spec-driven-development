package com.example.blog;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cliente HTTP dos testes de integração, contra o servidor real em porta aleatória.
 */
public class ApiTestClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final String baseUrl;

    public ApiTestClient(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    public static String uniqueUsername() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String emailOf(String username) {
        return username + "@example.com";
    }

    public ApiResponse send(String method, String path, String token, String body)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Token " + token);
        }
        HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json = response.body().isEmpty() ? null : jsonMapper.readTree(response.body());
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        return new ApiResponse(response.statusCode(), contentType, json);
    }

    /**
     * Cadastra um usuário fictício com a senha {@code password} e devolve o token.
     */
    public String register(String username) throws IOException, InterruptedException {
        ApiResponse response = send("POST", "/api/users", null, """
                {"user": {"username": "%s", "email": "%s", "password": "password"}}
                """.formatted(username, emailOf(username)));
        if (response.status() != 201) {
            throw new IllegalStateException("Cadastro de teste falhou com status " + response.status());
        }
        return response.json().path("user").path("token").asString();
    }

    public record ApiResponse(int status, String contentType, JsonNode json) {
    }
}
