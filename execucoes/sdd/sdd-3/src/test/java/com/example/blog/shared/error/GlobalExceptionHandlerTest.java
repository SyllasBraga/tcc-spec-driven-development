package com.example.blog.shared.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import com.example.blog.shared.security.ErrorAccessDeniedHandler;
import com.example.blog.shared.security.ErrorAuthenticationEntryPoint;
import com.example.blog.shared.security.JwtProperties;
import com.example.blog.shared.security.SecurityConfig;
import com.example.blog.shared.security.TokenService;
import com.example.blog.shared.security.UsuarioAtual;
import com.example.blog.shared.time.ClockConfig;
import com.example.blog.user.service.UserService;
import com.example.blog.user.web.UserController;

/**
 * Erros transversais: rota inexistente, método não suportado, tipo de mídia e falha inesperada.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ClockConfig.class, ErrorAuthenticationEntryPoint.class,
        ErrorAccessDeniedHandler.class})
@EnableConfigurationProperties(JwtProperties.class)
class GlobalExceptionHandlerTest {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioAtual usuarioAtual;

    @Test
    void deveResponder404ParaRotaInexistente() throws Exception {
        mockMvc.perform(get("/api/nao-existe").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"errors":{"resource":["not found"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveResponder405ParaMetodoNaoSuportado() throws Exception {
        mockMvc.perform(put("/api/users/login").with(jwt()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"errors":{"method":["not allowed"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveResponder422ParaTipoDeMidiaNaoSuportado() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"errors":{"body":["is invalid"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveResponder500SemExporDetalhesInternos() throws Exception {
        when(userService.cadastrar(any())).thenThrow(new IllegalStateException("falha na tabela users"));

        String corpo = mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"user":{"username":"jake","email":"jake@jake.jake","password":"jakejake"}}
                        """))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"errors":{"server":["internal error"]}}
                        """, JsonCompareMode.STRICT))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).doesNotContain("users").doesNotContain("Exception");
    }
}
