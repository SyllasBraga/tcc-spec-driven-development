package com.example.blog.usuario.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.blog.shared.erro.TokenInvalidoException;
import com.example.blog.shared.seguranca.SegredoJwtAleatorioConfiguration;
import com.example.blog.shared.seguranca.SegurancaConfig;
import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.service.AlteracaoUsuario;
import com.example.blog.usuario.service.UsuarioAutenticado;
import com.example.blog.usuario.service.UsuarioService;

@WebMvcTest(UsuarioController.class)
@Import({SegurancaConfig.class, TokenService.class, AlteracaoUsuarioMapper.class,
        SegredoJwtAleatorioConfiguration.class})
class UsuarioControllerAtualTest {

    private static final long ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void deveResponder401IsMissingSemCabecalho() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    @Test
    void deveResponder401IsInvalidComEsquemaBearer() throws Exception {
        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.emitir(ID)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void deveResponder401IsInvalidComTokenMalformado() throws Exception {
        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Token nao.e.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void deveResponder401QuandoUsuarioDoTokenNaoExiste() throws Exception {
        String token = tokenService.emitir(ID);
        when(usuarioService.buscarAtual(ID, token)).thenThrow(new TokenInvalidoException());

        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Token " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void deveDevolverUsuarioAtual() throws Exception {
        String token = tokenService.emitir(ID);
        when(usuarioService.buscarAtual(ID, token))
                .thenReturn(new UsuarioAutenticado("ana@exemplo.test", token, "ana", null, null));

        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("ana"))
                .andExpect(jsonPath("$.user.token").value(token));
    }

    @Test
    void deveAtualizarUsuarioAtual() throws Exception {
        String token = tokenService.emitir(ID);
        when(usuarioService.atualizar(eq(ID), eq(token), any(AlteracaoUsuario.class)))
                .thenReturn(new UsuarioAutenticado("ana@exemplo.test", token, "ana", "Olá", null));

        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION, "Token " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"user":{"bio":"Olá","image":null}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value("Olá"))
                .andExpect(jsonPath("$.user.image").value((Object) null));
    }

    @Test
    void deveResponder422AoAtualizarSemCampos() throws Exception {
        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION, "Token " + tokenService.emitir(ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"user":{}}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.user[0]").value("must have at least one field"));
    }

    @Test
    void deveResponder401AoAtualizarSemCabecalho() throws Exception {
        mockMvc.perform(put("/api/user").contentType(MediaType.APPLICATION_JSON).content("""
                        {"user":{"bio":"x"}}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }
}
