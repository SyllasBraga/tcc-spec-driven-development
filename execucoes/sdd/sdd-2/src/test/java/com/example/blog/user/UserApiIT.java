package com.example.blog.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.TestcontainersConfiguration;
import com.example.blog.shared.security.TokenService;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserApiIT {

    private static final String SENHA = "senha-ficticia";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private TokenService tokenService;

    private record Conta(String username, String email, String token) {
    }

    private static String sufixoUnico() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String corpoCadastro(String username, String email, String password) {
        return """
                {"user":{"username":"%s","email":"%s","password":"%s"}}""".formatted(username, email, password);
    }

    private static String corpoLogin(String email, String password) {
        return """
                {"user":{"email":"%s","password":"%s"}}""".formatted(email, password);
    }

    private ResultActions postJson(String url, String corpo) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private Conta cadastrar() throws Exception {
        String sufixo = sufixoUnico();
        String username = "user-" + sufixo;
        String email = "email-" + sufixo + "@exemplo.test";
        MvcResult resultado = postJson("/api/users", corpoCadastro(username, email, SENHA))
                .andExpect(status().isCreated())
                .andReturn();
        String token = jsonMapper.readTree(resultado.getResponse().getContentAsString())
                .get("user").get("token").stringValue();
        return new Conta(username, email, token);
    }

    private static String token(String jwt) {
        return "Token " + jwt;
    }

    @Nested
    class Cadastro {

        @Test
        void deveCriarUsuarioERetornarUserComToken() throws Exception {
            String sufixo = sufixoUnico();
            String username = "Jacob-" + sufixo;
            String email = "jake-" + sufixo + "@jake.jake";

            postJson("/api/users", corpoCadastro(username, email, "jakejake"))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(header().string("Content-Type", containsStringIgnoringCase("charset=utf-8")))
                    .andExpect(jsonPath("$.user.email").value(email))
                    .andExpect(jsonPath("$.user.username").value(username))
                    .andExpect(jsonPath("$.user.token", not(emptyString())))
                    .andExpect(jsonPath("$.user.bio").value(nullValue()))
                    .andExpect(jsonPath("$.user.image").value(nullValue()))
                    .andExpect(jsonPath("$.user", hasKey("bio")))
                    .andExpect(jsonPath("$.user", hasKey("image")))
                    .andExpect(jsonPath("$.user.password").doesNotExist())
                    .andExpect(jsonPath("$.user.password_hash").doesNotExist())
                    .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
        }

        @Test
        void deveResponder409QuandoUsernameJaExiste() throws Exception {
            Conta existente = cadastrar();

            postJson("/api/users", corpoCadastro(existente.username(), "novo-" + sufixoUnico() + "@x.test", SENHA))
                    .andExpect(status().isConflict())
                    .andExpect(content().json("""
                            {"errors":{"username":["has already been taken"]}}""", STRICT));
        }

        @Test
        void deveResponder409QuandoEmailJaExisteComOutrasMaiusculas() throws Exception {
            Conta existente = cadastrar();

            postJson("/api/users", corpoCadastro("novo-" + sufixoUnico(), existente.email().toUpperCase(), SENHA))
                    .andExpect(status().isConflict())
                    .andExpect(content().json("""
                            {"errors":{"email":["has already been taken"]}}""", STRICT));
        }

        @Test
        void deveResponder409ComAsDuasChavesQuandoUsernameEEmailJaExistem() throws Exception {
            Conta existente = cadastrar();

            postJson("/api/users", corpoCadastro(existente.username(), existente.email(), SENHA))
                    .andExpect(status().isConflict())
                    .andExpect(content().json("""
                            {"errors":{"email":["has already been taken"],
                                       "username":["has already been taken"]}}""", STRICT));
        }

        @Test
        void deveResponder422QuandoEmailAusente() throws Exception {
            postJson("/api/users", """
                    {"user":{"username":"sem-email","password":"x"}}""")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(content().json("""
                            {"errors":{"email":["can't be blank"]}}""", STRICT));
        }

        @Test
        void deveResponder422QuandoEmailInvalido() throws Exception {
            postJson("/api/users", corpoCadastro("u-" + sufixoUnico(), "nao-e-email", SENHA))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(content().json("""
                            {"errors":{"email":["is invalid"]}}""", STRICT));
        }

        @Test
        void deveResponder422QuandoUsernameSoTemEspacos() throws Exception {
            postJson("/api/users", corpoCadastro("   ", "e-" + sufixoUnico() + "@x.test", SENHA))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(content().json("""
                            {"errors":{"username":["can't be blank"]}}""", STRICT));
        }

        @Test
        void deveResponder422BodyParaJsonMalformado() throws Exception {
            postJson("/api/users", "{")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(content().json("""
                            {"errors":{"body":["can't be empty"]}}""", STRICT));
        }

        @Test
        void deveResponder422BodyParaCorpoVazio() throws Exception {
            postJson("/api/users", "")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(content().json("""
                            {"errors":{"body":["can't be empty"]}}""", STRICT));
        }

        @Test
        void deveResponder422BodySemObjetoUser() throws Exception {
            postJson("/api/users", "{}")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(content().json("""
                            {"errors":{"body":["can't be empty"]}}""", STRICT));
        }
    }

    @Nested
    class Login {

        @Test
        void deveAutenticarERetornarUserComToken() throws Exception {
            Conta conta = cadastrar();

            postJson("/api/users/login", corpoLogin(conta.email(), SENHA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.email").value(conta.email()))
                    .andExpect(jsonPath("$.user.username").value(conta.username()))
                    .andExpect(jsonPath("$.user.token", not(emptyString())))
                    .andExpect(jsonPath("$.user", hasKey("bio")))
                    .andExpect(jsonPath("$.user", hasKey("image")));
        }

        @Test
        void deveAutenticarComEmailEmMaiusculas() throws Exception {
            Conta conta = cadastrar();

            postJson("/api/users/login", corpoLogin(conta.email().toUpperCase(), SENHA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.username").value(conta.username()));
        }

        @Test
        void deveResponder401ParaSenhaErrada() throws Exception {
            Conta conta = cadastrar();

            postJson("/api/users/login", corpoLogin(conta.email(), "senha-errada"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("""
                            {"errors":{"credentials":["invalid"]}}""", STRICT));
        }

        @Test
        void deveResponder401ParaEmailInexistente() throws Exception {
            postJson("/api/users/login", corpoLogin("ninguem-" + sufixoUnico() + "@x.test", SENHA))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("""
                            {"errors":{"credentials":["invalid"]}}""", STRICT));
        }

        @Test
        void deveResponder422SemPassword() throws Exception {
            postJson("/api/users/login", """
                    {"user":{"email":"a@b.test"}}""")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(content().json("""
                            {"errors":{"password":["can't be blank"]}}""", STRICT));
        }

        @Test
        void deveResponder422SemEmail() throws Exception {
            postJson("/api/users/login", """
                    {"user":{"password":"x"}}""")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(content().json("""
                            {"errors":{"email":["can't be blank"]}}""", STRICT));
        }

        @Test
        void deveIgnorarTokenInvalidoNoLogin() throws Exception {
            Conta conta = cadastrar();

            mockMvc.perform(post("/api/users/login")
                            .header("Authorization", "Token invalido")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoLogin(conta.email(), SENHA)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class UsuarioAtual {

        @Test
        void deveRetornarUsuarioAutenticado() throws Exception {
            Conta conta = cadastrar();

            MvcResult resultado = mockMvc.perform(get("/api/user").header("Authorization", token(conta.token())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.username").value(conta.username()))
                    .andExpect(jsonPath("$.user.email").value(conta.email()))
                    .andExpect(jsonPath("$.user.token", not(emptyString())))
                    .andReturn();

            String novoToken = jsonMapper.readTree(resultado.getResponse().getContentAsString())
                    .get("user").get("token").stringValue();
            assertThat(jwtDecoder.decode(novoToken).getSubject())
                    .isEqualTo(jwtDecoder.decode(conta.token()).getSubject());
        }

        @Test
        void deveResponder401IsMissingSemCabecalho() throws Exception {
            mockMvc.perform(get("/api/user"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("""
                            {"errors":{"token":["is missing"]}}""", STRICT));
        }

        @Test
        void deveResponder401IsMissingComEsquemaBearer() throws Exception {
            Conta conta = cadastrar();

            mockMvc.perform(get("/api/user").header("Authorization", "Bearer " + conta.token()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("""
                            {"errors":{"token":["is missing"]}}""", STRICT));
        }

        @Test
        void deveResponder401IsInvalidParaTokenMalformado() throws Exception {
            mockMvc.perform(get("/api/user").header("Authorization", "Token abc.def.ghi"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("""
                            {"errors":{"token":["is invalid"]}}""", STRICT));
        }

        @Test
        void deveResponder401IsInvalidParaTokenExpirado() throws Exception {
            Conta conta = cadastrar();
            String sujeito = jwtDecoder.decode(conta.token()).getSubject();
            Instant passado = Instant.now().minus(Duration.ofDays(3));
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .subject(sujeito)
                    .issuer("conduit-blog")
                    .issuedAt(passado)
                    .expiresAt(passado.plus(Duration.ofHours(24)))
                    .build();
            String expirado = jwtEncoder.encode(JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

            mockMvc.perform(get("/api/user").header("Authorization", token(expirado)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("""
                            {"errors":{"token":["is invalid"]}}""", STRICT));
        }

        @Test
        void deveResponder401IsInvalidParaUsuarioInexistente() throws Exception {
            String tokenOrfao = tokenService.emitir(Long.MAX_VALUE);

            mockMvc.perform(get("/api/user").header("Authorization", token(tokenOrfao)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("""
                            {"errors":{"token":["is invalid"]}}""", STRICT));
        }

        @Test
        void deveAtualizarBioEImageEPersistir() throws Exception {
            Conta conta = cadastrar();

            mockMvc.perform(put("/api/user")
                            .header("Authorization", token(conta.token()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"user":{"bio":"I like to skateboard","image":"https://i.stack.imgur.com/xHWG8.jpg"}}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.bio").value("I like to skateboard"))
                    .andExpect(jsonPath("$.user.image").value("https://i.stack.imgur.com/xHWG8.jpg"))
                    .andExpect(jsonPath("$.user.username").value(conta.username()))
                    .andExpect(jsonPath("$.user.email").value(conta.email()));

            mockMvc.perform(get("/api/user").header("Authorization", token(conta.token())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.bio").value("I like to skateboard"))
                    .andExpect(jsonPath("$.user.image").value("https://i.stack.imgur.com/xHWG8.jpg"));
        }

        @Test
        void deveLimparBioEnviadaComoNula() throws Exception {
            Conta conta = cadastrar();
            mockMvc.perform(put("/api/user")
                    .header("Authorization", token(conta.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"user":{"bio":"temporaria"}}"""))
                    .andExpect(status().isOk());

            mockMvc.perform(put("/api/user")
                            .header("Authorization", token(conta.token()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"user":{"bio":null}}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.bio").value(nullValue()))
                    .andExpect(jsonPath("$.user", hasKey("bio")));
        }

        @Test
        void deveResponder422QuandoUserVazio() throws Exception {
            Conta conta = cadastrar();

            mockMvc.perform(put("/api/user")
                            .header("Authorization", token(conta.token()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"user":{}}"""))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(content().json("""
                            {"errors":{"user":["can't be empty"]}}""", STRICT));
        }

        @Test
        void deveResponder409QuandoEmailPertenceAOutroUsuario() throws Exception {
            Conta conta = cadastrar();
            Conta outra = cadastrar();

            mockMvc.perform(put("/api/user")
                            .header("Authorization", token(conta.token()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"user":{"email":"%s"}}""".formatted(outra.email())))
                    .andExpect(status().isConflict())
                    .andExpect(content().json("""
                            {"errors":{"email":["has already been taken"]}}""", STRICT));
        }

        @Test
        void deveAceitarSomenteANovaSenhaAposAlteracao() throws Exception {
            Conta conta = cadastrar();

            mockMvc.perform(put("/api/user")
                            .header("Authorization", token(conta.token()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"user":{"password":"nova-senha-ficticia"}}"""))
                    .andExpect(status().isOk());

            postJson("/api/users/login", corpoLogin(conta.email(), SENHA))
                    .andExpect(status().isUnauthorized());
            postJson("/api/users/login", corpoLogin(conta.email(), "nova-senha-ficticia"))
                    .andExpect(status().isOk());
        }

        @Test
        void deveResponder401NoPutSemToken() throws Exception {
            mockMvc.perform(put("/api/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"user":{"bio":"x"}}"""))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("""
                            {"errors":{"token":["is missing"]}}""", STRICT));
        }
    }
}
