package com.example.blog;

import com.example.blog.shared.seguranca.TokenProperties;
import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.MissingNode;

/**
 * Apoio aos testes de integração: limpeza do banco, criação de usuários e tokens fictícios e um
 * cliente HTTP simples que devolve status, {@code Content-Type} e corpo JSON.
 */
@TestComponent
public class ApoioDeTeste {

    private static final String OUTRO_SEGREDO = "outro-segredo-ficticio-de-teste-0123456789abcdef";

    private final JdbcTemplate jdbcTemplate;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final TokenProperties tokenProperties;
    private final JsonMapper jsonMapper;
    private final Environment environment;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ApoioDeTeste(JdbcTemplate jdbcTemplate, UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder, TokenService tokenService, TokenProperties tokenProperties,
            JsonMapper jsonMapper, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.tokenProperties = tokenProperties;
        this.jsonMapper = jsonMapper;
        this.environment = environment;
    }

    public void limparBanco() {
        Boolean existeSeguimento = jdbcTemplate.queryForObject(
                "select to_regclass('public.seguimento') is not null", Boolean.class);
        if (Boolean.TRUE.equals(existeSeguimento)) {
            jdbcTemplate.update("delete from public.seguimento");
        }
        jdbcTemplate.update("delete from public.usuario");
    }

    public Usuario criarUsuario(String username, String email, String senha) {
        return usuarioRepository.saveAndFlush(
                new Usuario(username, email, passwordEncoder.encode(senha), Instant.now()));
    }

    public String tokenDe(Usuario usuario) {
        return tokenService.emitir(usuario.getId());
    }

    public String tokenExpirado(Long id) {
        Instant emitidoEm = Instant.now().minus(Duration.ofDays(2));
        return assinar(tokenProperties.segredo(), id, emitidoEm, emitidoEm.plus(Duration.ofDays(1)));
    }

    public String tokenComOutroSegredo(Long id) {
        Instant agora = Instant.now();
        return assinar(OUTRO_SEGREDO, id, agora, agora.plus(Duration.ofHours(1)));
    }

    public String cabecalhoToken(String token) {
        return "Token " + token;
    }

    public long contarLinhas(String tabela) {
        Long total = jdbcTemplate.queryForObject("select count(*) from public." + tabela, Long.class);
        return total == null ? 0 : total;
    }

    public JsonNode json(String texto) {
        return jsonMapper.readTree(texto);
    }

    public RespostaHttp get(String caminho, String autorizacao) {
        return enviar("GET", caminho, null, autorizacao);
    }

    public RespostaHttp post(String caminho, String corpo) {
        return enviar("POST", caminho, corpo, null);
    }

    public RespostaHttp post(String caminho, String corpo, String autorizacao) {
        return enviar("POST", caminho, corpo, autorizacao);
    }

    public RespostaHttp put(String caminho, String corpo, String autorizacao) {
        return enviar("PUT", caminho, corpo, autorizacao);
    }

    public RespostaHttp delete(String caminho, String autorizacao) {
        return enviar("DELETE", caminho, null, autorizacao);
    }

    private RespostaHttp enviar(String metodo, String caminho, String corpo, String autorizacao) {
        String porta = environment.getRequiredProperty("local.server.port");
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .method(metodo, corpo == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8));
        if (corpo != null) {
            requisicao.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }
        if (autorizacao != null) {
            requisicao.header(HttpHeaders.AUTHORIZATION, autorizacao);
        }
        try {
            HttpResponse<String> resposta = httpClient.send(requisicao.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            MediaType tipo = resposta.headers().firstValue(HttpHeaders.CONTENT_TYPE)
                    .map(MediaType::parseMediaType)
                    .orElse(null);
            JsonNode json = resposta.body().isBlank() ? MissingNode.getInstance() : jsonMapper.readTree(resposta.body());
            return new RespostaHttp(resposta.statusCode(), tipo, json);
        } catch (IOException excecao) {
            throw new IllegalStateException("Falha na requisição " + metodo + " " + caminho, excecao);
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Requisição interrompida " + metodo + " " + caminho, excecao);
        }
    }

    private String assinar(String segredo, Long id, Instant emitidoEm, Instant expiraEm) {
        var chave = new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(id))
                .issuer(tokenProperties.emissor())
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .build();
        return NimbusJwtEncoder.withSecretKey(chave).build()
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
