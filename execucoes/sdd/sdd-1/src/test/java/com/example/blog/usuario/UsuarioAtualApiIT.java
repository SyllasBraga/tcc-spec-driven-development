package com.example.blog.usuario;

import static com.example.blog.ApiDeTeste.SENHA;
import static com.example.blog.ApiDeTeste.autorizacao;
import static com.example.blog.ApiDeTeste.emailDe;
import static com.example.blog.ApiDeTeste.json;
import static com.example.blog.ApiDeTeste.usernameUnico;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.blog.ApiDeTeste;
import com.example.blog.TesteDeIntegracao;
import com.example.blog.shared.security.JwtProperties;
import com.example.blog.shared.security.TokenService;

/**
 * US2 ponta a ponta: consulta e atualização do usuário autenticado.
 */
@TesteDeIntegracao
class UsuarioAtualApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    private ApiDeTeste api;

    @BeforeEach
    void configurar() {
        api = new ApiDeTeste(mockMvc);
    }

    @Test
    void deveRetornarUsuarioDonoDoToken() throws Exception {
        String username = usernameUnico("ana");
        String token = api.registrarETomarToken(username);

        buscarAtual(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.email").value(emailDe(username)))
                .andExpect(jsonPath("$.user.token").value(token));
    }

    @Test
    void deveAtualizarBioPreservandoDemaisCampos() throws Exception {
        String username = usernameUnico("ana");
        String token = api.registrarETomarToken(username);

        atualizar(token, "{\"user\":{\"bio\":\"Olá\"}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value("Olá"))
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.email").value(emailDe(username)));

        buscarAtual(token).andExpect(jsonPath("$.user.bio").value("Olá"));
    }

    @Test
    void deveLimparBioInformadaNula() throws Exception {
        String token = api.registrarETomarToken(usernameUnico("ana"));
        atualizar(token, "{\"user\":{\"bio\":\"Olá\"}}").andExpect(status().isOk());

        atualizar(token, "{\"user\":{\"bio\":null}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value(nullValue()));
    }

    @Test
    void deveLimparBioEImageInformadasVazias() throws Exception {
        String token = api.registrarETomarToken(usernameUnico("ana"));
        atualizar(token, "{\"user\":{\"bio\":\"Olá\",\"image\":\"https://exemplo.test/a.png\"}}")
                .andExpect(status().isOk());

        atualizar(token, "{\"user\":{\"bio\":\"\",\"image\":\"\"}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value(nullValue()))
                .andExpect(jsonPath("$.user.image").value(nullValue()));
        buscarAtual(token)
                .andExpect(jsonPath("$.user.bio").value(nullValue()))
                .andExpect(jsonPath("$.user.image").value(nullValue()));
    }

    @Test
    void deveTrocarSenha() throws Exception {
        String username = usernameUnico("ana");
        String token = api.registrarETomarToken(username);

        atualizar(token, "{\"user\":{\"password\":\"nova-senha-ficticia\"}}").andExpect(status().isOk());

        api.entrar(emailDe(username), "nova-senha-ficticia").andExpect(status().isOk());
        api.entrar(emailDe(username), SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    void deveRecusarSenhaComMenosDeOitoCaracteres() throws Exception {
        String username = usernameUnico("ana");
        String token = api.registrarETomarToken(username);

        atualizar(token, "{\"user\":{\"password\":\"short7c\"}}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.password[0]").value("is too short (minimum is 8 characters)"));
        api.entrar(emailDe(username), SENHA).andExpect(status().isOk());
    }

    @Test
    void deveManterTokenValidoDepoisDeTrocarUsername() throws Exception {
        String token = api.registrarETomarToken(usernameUnico("ana"));
        String novoUsername = usernameUnico("ana-nova");

        atualizar(token, "{\"user\":{\"username\":\"" + novoUsername + "\"}}").andExpect(status().isOk());

        buscarAtual(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value(novoUsername));
    }

    @Test
    void deveRecusarSemAuthorization() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    @Test
    void deveRecusarTokenMalformado() throws Exception {
        buscarAtual("abc")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void deveRecusarTokenExpirado() throws Exception {
        Instant emissao = Instant.now().minus(jwtProperties.validade()).minusSeconds(60);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("1")
                .issuer(jwtProperties.issuer())
                .issuedAt(emissao)
                .expiresAt(emissao.plus(jwtProperties.validade()))
                .build();
        String tokenExpirado = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        buscarAtual(tokenExpirado)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void deveRecusarTokenDeUsuarioInexistente() throws Exception {
        buscarAtual(tokenService.emitir(Long.MAX_VALUE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void deveRecusarAtualizacaoSemCampos() throws Exception {
        String token = api.registrarETomarToken(usernameUnico("ana"));

        atualizar(token, "{\"user\":{}}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.user[0]").value("at least one field is required"));
    }

    @Test
    void deveRecusarUsernameDeOutroUsuarioCom422() throws Exception {
        String outro = usernameUnico("bia");
        api.registrarETomarToken(outro);
        String token = api.registrarETomarToken(usernameUnico("ana"));

        atualizar(token, "{\"user\":{\"username\":\"" + outro + "\"}}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.username[0]").value("has already been taken"));
    }

    @Test
    void deveRecusarJsonMalformado() throws Exception {
        String token = api.registrarETomarToken(usernameUnico("ana"));

        atualizar(token, "{\"user\":")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.body").exists());
    }

    private ResultActions buscarAtual(String token) throws Exception {
        return mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, autorizacao(token)));
    }

    private ResultActions atualizar(String token, String corpo) throws Exception {
        return mockMvc.perform(json(put("/api/user"), corpo).header(HttpHeaders.AUTHORIZATION, autorizacao(token)));
    }
}
