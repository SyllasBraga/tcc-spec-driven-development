package com.example.blog.usuario.web;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.blog.shared.config.ClockConfig;
import com.example.blog.shared.security.SecurityConfig;
import com.example.blog.shared.web.ConflitoException;
import com.example.blog.shared.web.CredenciaisInvalidasException;
import com.example.blog.usuario.service.UsuarioAutenticado;
import com.example.blog.usuario.service.UsuarioService;

@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, ClockConfig.class})
@ActiveProfiles("test")
class UsuarioControllerTest {

    private static final String CADASTRO_VALIDO =
            "{\"user\":{\"username\":\"jake\",\"email\":\"jake@x.com\",\"password\":\"senha-ficticia\"}}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void deveCadastrarERetornar201ComCorpoUser() throws Exception {
        when(usuarioService.registrar("jake", "jake@x.com", "senha-ficticia"))
                .thenReturn(new UsuarioAutenticado("jake@x.com", "jwt", "jake", null, null));

        postar("/api/users", CADASTRO_VALIDO)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/user"))
                .andExpect(jsonPath("$.user.email").value("jake@x.com"))
                .andExpect(jsonPath("$.user.token").value("jwt"))
                .andExpect(jsonPath("$.user.username").value("jake"))
                .andExpect(jsonPath("$.user", hasKey("bio")))
                .andExpect(jsonPath("$.user.bio").value(nullValue()))
                .andExpect(jsonPath("$.user", hasKey("image")));
    }

    @Test
    void deveRecusarCadastroSemEmail() throws Exception {
        postar("/api/users", "{\"user\":{\"username\":\"jake\",\"password\":\"senha-ficticia\"}}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("can't be blank"));
    }

    @Test
    void deveRecusarCadastroComEmailInvalido() throws Exception {
        postar("/api/users", "{\"user\":{\"username\":\"jake\",\"email\":\"x\",\"password\":\"senha-ficticia\"}}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("is invalid"));
    }

    @Test
    void deveRecusarCadastroSemOsTresCampos() throws Exception {
        postar("/api/users", "{\"user\":{}}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.username[0]").value("can't be blank"))
                .andExpect(jsonPath("$.errors.email[0]").value("can't be blank"))
                .andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
    }

    @Test
    void deveRecusarJsonMalformado() throws Exception {
        postar("/api/users", "{\"user\":")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.body").exists());
    }

    @Test
    void deveRecusarCorpoSemObjetoUser() throws Exception {
        postar("/api/users", "{}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.body").exists());
    }

    @Test
    void deveResponder409QuandoUsernameJaUsado() throws Exception {
        when(usuarioService.registrar(anyString(), anyString(), anyString()))
                .thenThrow(new ConflitoException("username", "has already been taken"));

        postar("/api/users", CADASTRO_VALIDO)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.username[0]").value("has already been taken"));
    }

    @Test
    void deveEntrarERetornar200() throws Exception {
        when(usuarioService.entrar("jake@x.com", "senha-ficticia"))
                .thenReturn(new UsuarioAutenticado("jake@x.com", "jwt", "jake", null, null));

        postar("/api/users/login", "{\"user\":{\"email\":\"jake@x.com\",\"password\":\"senha-ficticia\"}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.token").value("jwt"));
    }

    @Test
    void deveRecusarLoginSemPassword() throws Exception {
        postar("/api/users/login", "{\"user\":{\"email\":\"jake@x.com\"}}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
    }

    @Test
    void deveResponder401ComCredenciaisInvalidas() throws Exception {
        when(usuarioService.entrar(anyString(), anyString())).thenThrow(new CredenciaisInvalidasException());

        postar("/api/users/login", "{\"user\":{\"email\":\"jake@x.com\",\"password\":\"errada\"}}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.credentials[0]").value("invalid"));
    }

    private ResultActions postar(String rota, String corpo) throws Exception {
        return mockMvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }
}
