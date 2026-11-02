package com.example.blog.perfil.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.blog.perfil.service.PerfilDto;
import com.example.blog.perfil.service.PerfilService;
import com.example.blog.shared.erro.ManipuladorDeErros;
import com.example.blog.shared.erro.RecursoNaoEncontradoException;
import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.shared.seguranca.ErroAutenticacaoHandler;
import com.example.blog.shared.seguranca.SegurancaConfig;
import com.example.blog.shared.seguranca.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(PerfilController.class)
@Import({SegurancaConfig.class, ErroAutenticacaoHandler.class, ManipuladorDeErros.class, TokenService.class})
@ActiveProfiles("test")
class PerfilControllerTest {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";

    private final MockMvcTester mvc;
    private final TokenService tokenService;

    @MockitoBean
    private PerfilService perfilService;

    PerfilControllerTest(@Autowired MockMvcTester mvc, @Autowired TokenService tokenService) {
        this.mvc = mvc;
        this.tokenService = tokenService;
    }

    @Test
    void deveDevolverPerfilSemTokenComQuatroCampos() {
        when(perfilService.buscarPerfil("jake", null)).thenReturn(new PerfilDto("jake", null, null, false));

        MvcTestResult resultado = mvc.get().uri("/api/profiles/jake").exchange();

        assertThat(resultado).hasStatus(200).hasContentType(JSON_UTF8);
        assertThat(resultado).bodyJson().isStrictlyEqualTo("""
                {"profile":{"username":"jake","bio":null,"image":null,"following":false}}""");
    }

    @Test
    void deveRepassarUsuarioAutenticadoNaRotaOpcional() {
        when(perfilService.buscarPerfil("jake", 7L)).thenReturn(new PerfilDto("jake", "bio", "img", true));

        MvcTestResult resultado = mvc.get().uri("/api/profiles/jake")
                .header(HttpHeaders.AUTHORIZATION, "Token " + tokenService.emitir(7L))
                .exchange();

        assertThat(resultado).hasStatus(200);
        assertThat(resultado).bodyJson().isStrictlyEqualTo("""
                {"profile":{"username":"jake","bio":"bio","image":"img","following":true}}""");
        verify(perfilService).buscarPerfil("jake", 7L);
    }

    @Test
    void deveDevolver404ParaPerfilInexistente() {
        when(perfilService.buscarPerfil("ninguem", null)).thenThrow(new RecursoNaoEncontradoException("profile"));

        MvcTestResult resultado = mvc.get().uri("/api/profiles/ninguem").exchange();

        assertThat(resultado).hasStatus(404).hasContentType(JSON_UTF8);
        assertThat(resultado).bodyJson().isStrictlyEqualTo("""
                {"errors":{"profile":["not found"]}}""");
    }

    @Test
    void deveExigirTokenParaSeguir() {
        MvcTestResult resultado = mvc.post().uri("/api/profiles/jake/follow").exchange();

        assertThat(resultado).hasStatus(401).hasContentType(JSON_UTF8);
        assertThat(resultado).bodyJson().isStrictlyEqualTo("""
                {"errors":{"token":["is missing"]}}""");
        verifyNoInteractions(perfilService);
    }

    @Test
    void deveRecusarTokenInvalidoNaRotaOpcional() {
        MvcTestResult resultado = mvc.get().uri("/api/profiles/jake")
                .header(HttpHeaders.AUTHORIZATION, "Token abc.def.ghi")
                .exchange();

        assertThat(resultado).hasStatus(401).hasContentType(JSON_UTF8);
        assertThat(resultado).bodyJson().isStrictlyEqualTo("""
                {"errors":{"token":["is invalid"]}}""");
    }

    @Test
    void deveDevolver422AoSeguirASiMesmo() {
        when(perfilService.seguir("jake", 7L))
                .thenThrow(ValidacaoException.de("profile", "cannot follow yourself"));

        MvcTestResult resultado = mvc.post().uri("/api/profiles/jake/follow")
                .header(HttpHeaders.AUTHORIZATION, "Token " + tokenService.emitir(7L))
                .exchange();

        assertThat(resultado).hasStatus(422).hasContentType(JSON_UTF8);
        assertThat(resultado).bodyJson().isStrictlyEqualTo("""
                {"errors":{"profile":["cannot follow yourself"]}}""");
    }

    @Test
    void deveDeixarDeSeguirCom200() {
        when(perfilService.deixarDeSeguir("jake", 7L)).thenReturn(new PerfilDto("jake", null, null, false));

        MvcTestResult resultado = mvc.delete().uri("/api/profiles/jake/follow")
                .header(HttpHeaders.AUTHORIZATION, "Token " + tokenService.emitir(7L))
                .exchange();

        assertThat(resultado).hasStatus(200).hasContentType(JSON_UTF8);
        assertThat(resultado).bodyJson().isStrictlyEqualTo("""
                {"profile":{"username":"jake","bio":null,"image":null,"following":false}}""");
    }
}
