package com.example.blog.user.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.blog.shared.error.GlobalExceptionHandler;
import com.example.blog.shared.security.ErrorAccessDeniedHandler;
import com.example.blog.shared.security.ErrorAuthenticationEntryPoint;
import com.example.blog.shared.security.JwtProperties;
import com.example.blog.shared.security.SecurityConfig;
import com.example.blog.shared.security.TokenService;
import com.example.blog.shared.security.UsuarioAtual;
import com.example.blog.shared.time.ClockConfig;
import com.example.blog.user.service.AtualizacaoUsuario;
import com.example.blog.user.service.AtualizacaoVaziaException;
import com.example.blog.user.service.CredenciaisInvalidasException;
import com.example.blog.user.service.UserService;
import com.example.blog.user.service.UsuarioAutenticado;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ClockConfig.class, ErrorAuthenticationEntryPoint.class,
        ErrorAccessDeniedHandler.class})
@EnableConfigurationProperties(JwtProperties.class)
class UserControllerTest {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";
    private static final UsuarioAutenticado JAKE =
            new UsuarioAutenticado("jake@jake.jake", "tok", "jake", null, null);
    private static final String CORPO_JAKE = """
            {"user":{"email":"jake@jake.jake","token":"tok","username":"jake","bio":null,"image":null}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioAtual usuarioAtual;

    // ---------- POST /api/users ----------

    @Test
    void deveCadastrarComStatus201() throws Exception {
        when(userService.cadastrar(any())).thenReturn(JAKE);

        MvcResult resultado = mockMvc.perform(json(post("/api/users"), """
                        {"user":{"username":"jake","email":"jake@jake.jake","password":"jakejake"}}
                        """))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json(CORPO_JAKE, JsonCompareMode.STRICT))
                .andReturn();

        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("password");
    }

    @Test
    void deveRecusarCadastroSemEmail() throws Exception {
        mockMvc.perform(json(post("/api/users"), """
                        {"user":{"username":"jake","password":"jakejake"}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"errors":{"email":["can't be blank"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveRecusarCadastroComEmailInvalido() throws Exception {
        mockMvc.perform(json(post("/api/users"), """
                        {"user":{"username":"jake","email":"jake","password":"jakejake"}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"email":["is invalid"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveRecusarCorpoSemObjetoUser() throws Exception {
        mockMvc.perform(json(post("/api/users"), "{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"body":["can't be empty"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveRecusarJsonMalformado() throws Exception {
        mockMvc.perform(json(post("/api/users"), "{\"user\":"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"errors":{"body":["can't be empty"]}}
                        """, JsonCompareMode.STRICT));
    }

    // ---------- POST /api/users/login ----------

    @Test
    void deveEntrarComStatus200() throws Exception {
        when(userService.autenticar(any())).thenReturn(JAKE);

        mockMvc.perform(json(post("/api/users/login"), """
                        {"user":{"email":"jake@jake.jake","password":"jakejake"}}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json(CORPO_JAKE, JsonCompareMode.STRICT));
    }

    @Test
    void deveRecusarLoginSemSenha() throws Exception {
        mockMvc.perform(json(post("/api/users/login"), """
                        {"user":{"email":"jake@jake.jake"}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"password":["can't be blank"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveResponder401ComCredenciaisInvalidas() throws Exception {
        when(userService.autenticar(any())).thenThrow(new CredenciaisInvalidasException());

        mockMvc.perform(json(post("/api/users/login"), """
                        {"user":{"email":"jake@jake.jake","password":"errada"}}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"errors":{"credentials":["invalid"]}}
                        """, JsonCompareMode.STRICT));
    }

    // ---------- GET /api/user ----------

    @Test
    void deveResponder401SemCabecalho() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"errors":{"token":["is missing"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveResponder401ComEsquemaBearer() throws Exception {
        mockMvc.perform(get("/api/user").header("Authorization", "Bearer x"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"errors":{"token":["is invalid"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveBuscarUsuarioAutenticado() throws Exception {
        when(usuarioAtual.idObrigatorio()).thenReturn(1L);
        when(userService.buscarAtual(1L)).thenReturn(JAKE);

        mockMvc.perform(get("/api/user").with(jwt().jwt(j -> j.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json(CORPO_JAKE, JsonCompareMode.STRICT));
    }

    // ---------- PUT /api/user ----------

    @Test
    void deveConverterBioNulaEmTextoVazio() throws Exception {
        when(usuarioAtual.idObrigatorio()).thenReturn(1L);
        when(userService.atualizar(eq(1L), any())).thenReturn(JAKE);

        mockMvc.perform(json(put("/api/user"), """
                        {"user":{"bio":null}}
                        """).with(jwt()))
                .andExpect(status().isOk());

        ArgumentCaptor<AtualizacaoUsuario> atualizacao = ArgumentCaptor.forClass(AtualizacaoUsuario.class);
        verify(userService).atualizar(eq(1L), atualizacao.capture());
        assertThat(atualizacao.getValue().bio()).isEmpty();
        assertThat(atualizacao.getValue().image()).isNull();
    }

    @Test
    void deveResponder422ComAtualizacaoVazia() throws Exception {
        when(usuarioAtual.idObrigatorio()).thenReturn(1L);
        when(userService.atualizar(eq(1L), any())).thenThrow(new AtualizacaoVaziaException());

        mockMvc.perform(json(put("/api/user"), """
                        {"user":{}}
                        """).with(jwt()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"body":["can't be empty"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveRecusarEmailInvalidoNaAtualizacao() throws Exception {
        mockMvc.perform(json(put("/api/user"), """
                        {"user":{"email":"invalido"}}
                        """).with(jwt()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"email":["is invalid"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveRecusarUsernameEmBrancoNaAtualizacao() throws Exception {
        mockMvc.perform(json(put("/api/user"), """
                        {"user":{"username":"  "}}
                        """).with(jwt()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"username":["can't be blank"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveRecusarEmailNuloNaAtualizacao() throws Exception {
        mockMvc.perform(json(put("/api/user"), """
                        {"user":{"email":null,"bio":"I like to skateboard"}}
                        """).with(jwt()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"email":["can't be blank"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveRecusarUsernameESenhaNulosNaAtualizacao() throws Exception {
        mockMvc.perform(json(put("/api/user"), """
                        {"user":{"username":null,"password":null}}
                        """).with(jwt()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"username":["can't be blank"],"password":["can't be blank"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveRecusarCampoQueNaoETextoNaAtualizacao() throws Exception {
        mockMvc.perform(json(put("/api/user"), """
                        {"user":{"username":123}}
                        """).with(jwt()))
                .andExpect(status().isUnprocessableContent());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String corpo) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }
}
