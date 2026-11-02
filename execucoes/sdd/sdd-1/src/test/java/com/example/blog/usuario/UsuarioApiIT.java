package com.example.blog.usuario;

import static com.example.blog.ApiDeTeste.SENHA;
import static com.example.blog.ApiDeTeste.emailDe;
import static com.example.blog.ApiDeTeste.json;
import static com.example.blog.ApiDeTeste.usernameUnico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.blog.ApiDeTeste;
import com.example.blog.TesteDeIntegracao;

/**
 * US1 ponta a ponta: cadastro e login com JWT.
 */
@TesteDeIntegracao
class UsuarioApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ApiDeTeste api;

    @BeforeEach
    void configurar() {
        api = new ApiDeTeste(mockMvc);
    }

    @Test
    void deveCadastrarComBioEImageNulosETokenPreenchido() throws Exception {
        String username = usernameUnico("ana");

        MvcResult resultado = api.registrar(username, emailDe(username), SENHA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.email").value(emailDe(username)))
                .andExpect(jsonPath("$.user.token").value(not(emptyOrNullString())))
                .andExpect(jsonPath("$.user", hasKey("bio")))
                .andExpect(jsonPath("$.user.bio").value(nullValue()))
                .andExpect(jsonPath("$.user", hasKey("image")))
                .andExpect(jsonPath("$.user.image").value(nullValue()))
                .andReturn();

        assertContentTypeJsonUtf8(resultado);
    }

    @Test
    void deveEntrarComCredenciaisCorretas() throws Exception {
        String username = usernameUnico("ana");
        api.registrar(username, emailDe(username), SENHA).andExpect(status().isCreated());

        MvcResult resultado = api.entrar(emailDe(username), SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.token").value(not(emptyOrNullString())))
                .andReturn();

        assertContentTypeJsonUtf8(resultado);
    }

    @Test
    void deveEntrarComEmailEmOutraCaixa() throws Exception {
        String username = usernameUnico("ana");
        api.registrar(username, emailDe(username), SENHA).andExpect(status().isCreated());

        api.entrar(emailDe(username).toUpperCase(Locale.ROOT), SENHA).andExpect(status().isOk());
    }

    @Test
    void deveRecusarLoginComSenhaErrada() throws Exception {
        String username = usernameUnico("ana");
        api.registrar(username, emailDe(username), SENHA).andExpect(status().isCreated());

        api.entrar(emailDe(username), "senha-errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.credentials[0]").value("invalid"));
    }

    @Test
    void deveRecusarUsernameRepetidoCom409() throws Exception {
        String username = usernameUnico("ana");
        api.registrar(username, emailDe(username), SENHA).andExpect(status().isCreated());

        api.registrar(username, emailDe(usernameUnico("outro")), SENHA)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.username[0]").value("has already been taken"));
    }

    @Test
    void deveTratarEmailComCaixaDiferenteComoRepetido() throws Exception {
        String username = usernameUnico("jake");
        api.registrar(username, "Jake-" + username + "@x.test", SENHA).andExpect(status().isCreated());

        api.registrar(usernameUnico("outro"), "jake-" + username + "@x.test", SENHA)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.email[0]").value("has already been taken"));
    }

    @Test
    void deveRecusarCadastroSemEmailCom422() throws Exception {
        mockMvc.perform(json(post("/api/users"),
                        "{\"user\":{\"username\":\"" + usernameUnico("ana") + "\",\"password\":\"" + SENHA + "\"}}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("can't be blank"));
    }

    @Test
    void deveGravarSomenteHashDaSenha() throws Exception {
        String username = usernameUnico("ana");
        api.registrar(username, emailDe(username), SENHA).andExpect(status().isCreated());

        String senhaHash = jdbcTemplate.queryForObject(
                "SELECT senha_hash FROM usuario WHERE username = ?", String.class, username);

        assertThat(senhaHash).startsWith("{bcrypt}").doesNotContain(SENHA);
    }

    private static void assertContentTypeJsonUtf8(MvcResult resultado) {
        String contentType = resultado.getResponse().getContentType().toLowerCase(Locale.ROOT).replace(" ", "");
        assertThat(contentType).startsWith("application/json").contains("charset=utf-8");
    }
}
