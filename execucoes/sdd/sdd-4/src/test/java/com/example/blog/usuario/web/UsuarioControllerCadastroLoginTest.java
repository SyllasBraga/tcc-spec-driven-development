package com.example.blog.usuario.web;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.blog.shared.erro.ConflitoException;
import com.example.blog.shared.erro.CredenciaisInvalidasException;
import com.example.blog.shared.seguranca.SegredoJwtAleatorioConfiguration;
import com.example.blog.shared.seguranca.SegurancaConfig;
import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.service.UsuarioAutenticado;
import com.example.blog.usuario.service.UsuarioService;

@WebMvcTest(UsuarioController.class)
@Import({SegurancaConfig.class, TokenService.class, AlteracaoUsuarioMapper.class,
        SegredoJwtAleatorioConfiguration.class})
class UsuarioControllerCadastroLoginTest {

    private static final UsuarioAutenticado ANA =
            new UsuarioAutenticado("ana@exemplo.test", "jwt.ficticio", "ana", null, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void deveCadastrarERetornar201ComUsuario() throws Exception {
        when(usuarioService.cadastrar("ana", "ana@exemplo.test", "senha1")).thenReturn(ANA);

        postar("/api/users", """
                {"user":{"username":"ana","email":"ana@exemplo.test","password":"senha1"}}""")
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user.email").value("ana@exemplo.test"))
                .andExpect(jsonPath("$.user.token").value("jwt.ficticio"))
                .andExpect(jsonPath("$.user.username").value("ana"))
                .andExpect(jsonPath("$.user.bio").value((Object) null))
                .andExpect(jsonPath("$.user.image").value((Object) null))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void deveRecusarCadastroComCamposAusentes() throws Exception {
        postar("/api/users", """
                {"user":{}}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.username[0]").value("can't be blank"))
                .andExpect(jsonPath("$.errors.email[0]").value("can't be blank"))
                .andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
    }

    @Test
    void deveRecusarCadastroComCamposEmBranco() throws Exception {
        postar("/api/users", """
                {"user":{"username":"  ","email":"   ","password":""}}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.username").value(contains("can't be blank")))
                .andExpect(jsonPath("$.errors.email").value(contains("can't be blank")))
                .andExpect(jsonPath("$.errors.password").value(contains("can't be blank")));
    }

    @Test
    void deveRecusarCadastroComEmailSemFormato() throws Exception {
        postar("/api/users", """
                {"user":{"username":"ana","email":"nao-e-email","password":"senha1"}}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("is invalid"));
    }

    @Test
    void deveRecusarJsonMalformado() throws Exception {
        postar("/api/users", "{\"user\":")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.body[0]").value("is invalid"));
    }

    @Test
    void deveRecusarCadastroSemObjetoUser() throws Exception {
        postar("/api/users", """
                {"username":"ana"}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.user[0]").value("can't be blank"));
    }

    @Test
    void deveResponder409QuandoUsernameJaExiste() throws Exception {
        when(usuarioService.cadastrar("ana", "ana@exemplo.test", "senha1"))
                .thenThrow(new ConflitoException("username", "has already been taken"));

        postar("/api/users", """
                {"user":{"username":"ana","email":"ana@exemplo.test","password":"senha1"}}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.username[0]").value("has already been taken"));
    }

    @Test
    void deveFazerLogin() throws Exception {
        when(usuarioService.autenticar("ana@exemplo.test", "senha1")).thenReturn(ANA);

        postar("/api/users/login", """
                {"user":{"email":"ana@exemplo.test","password":"senha1"}}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.token").value("jwt.ficticio"));
    }

    @Test
    void deveRecusarLoginComCamposEmBranco() throws Exception {
        postar("/api/users/login", """
                {"user":{"email":"","password":null}}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("can't be blank"))
                .andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
    }

    @Test
    void deveResponder401ComCredenciaisInvalidas() throws Exception {
        when(usuarioService.autenticar("ana@exemplo.test", "errada")).thenThrow(new CredenciaisInvalidasException());

        postar("/api/users/login", """
                {"user":{"email":"ana@exemplo.test","password":"errada"}}""")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.credentials[0]").value("invalid"));
    }

    private ResultActions postar(String rota, String corpo) throws Exception {
        return mockMvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }
}
