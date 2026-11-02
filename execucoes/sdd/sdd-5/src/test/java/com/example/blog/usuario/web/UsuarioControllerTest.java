package com.example.blog.usuario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.blog.shared.erro.CredenciaisInvalidasException;
import com.example.blog.shared.erro.ManipuladorDeErros;
import com.example.blog.shared.seguranca.ErroAutenticacaoHandler;
import com.example.blog.shared.seguranca.SegurancaConfig;
import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.service.AtualizacaoUsuario;
import com.example.blog.usuario.service.CampoAtualizado;
import com.example.blog.usuario.service.UsuarioAutenticadoDto;
import com.example.blog.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(UsuarioController.class)
@Import({SegurancaConfig.class, ErroAutenticacaoHandler.class, ManipuladorDeErros.class,
        AtualizacaoUsuarioMapper.class, TokenService.class})
@ActiveProfiles("test")
class UsuarioControllerTest {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";
    private static final UsuarioAutenticadoDto JAKE =
            new UsuarioAutenticadoDto(7L, "jake@jake.jake", "jake", null, null, "tok");
    private static final String USER_JAKE = """
            {"user":{"email":"jake@jake.jake","token":"tok","username":"jake","bio":null,"image":null}}""";

    private final MockMvcTester mvc;
    private final TokenService tokenService;

    @MockitoBean
    private UsuarioService usuarioService;

    UsuarioControllerTest(@Autowired MockMvcTester mvc, @Autowired TokenService tokenService) {
        this.mvc = mvc;
        this.tokenService = tokenService;
    }

    @Test
    void deveCadastrarComStatus201EObjetoUserCompleto() {
        when(usuarioService.cadastrar("jake", "jake@jake.jake", "jakejake")).thenReturn(JAKE);

        MvcTestResult resultado = mvc.post().uri("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"user":{"username":"jake","email":"jake@jake.jake","password":"jakejake"}}""")
                .exchange();

        assertThat(resultado).hasStatus(201).hasContentType(JSON_UTF8);
        assertThat(resultado).bodyJson().isStrictlyEqualTo(USER_JAKE);
        assertThat(resultado).bodyText().doesNotContain("jakejake").doesNotContain("password");
    }

    @Test
    void deveRecusarCadastroSemSenhaCom422() {
        MvcTestResult resultado = mvc.post().uri("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"user":{"username":"jake","email":"jake@jake.jake"}}""")
                .exchange();

        assertThat(resultado).hasStatus(422).hasContentType(JSON_UTF8);
        assertThat(resultado).bodyJson().isStrictlyEqualTo("""
                {"errors":{"password":["can't be blank"]}}""");
        verifyNoInteractions(usuarioService);
    }

    @Test
    void deveAutenticarCom200() {
        when(usuarioService.autenticar("jake@jake.jake", "jakejake")).thenReturn(JAKE);

        MvcTestResult resultado = mvc.post().uri("/api/users/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"user":{"email":"jake@jake.jake","password":"jakejake"}}""")
                .exchange();

        assertThat(resultado).hasStatus(200).hasContentType(JSON_UTF8);
        assertThat(resultado).bodyJson().isStrictlyEqualTo(USER_JAKE);
    }

    @Test
    void deveRecusarLoginInvalidoCom401() {
        when(usuarioService.autenticar(any(), any())).thenThrow(new CredenciaisInvalidasException());

        MvcTestResult resultado = mvc.post().uri("/api/users/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"user":{"email":"jake@jake.jake","password":"errada"}}""")
                .exchange();

        assertThat(resultado).hasStatus(401).hasContentType(JSON_UTF8);
        assertThat(resultado).bodyJson().isStrictlyEqualTo("""
                {"errors":{"credentials":["invalid"]}}""");
    }

    @Test
    void deveExigirTokenEmApiUser() {
        MvcTestResult resultado = mvc.get().uri("/api/user").exchange();

        assertThat(resultado).hasStatus(401).hasContentType(JSON_UTF8);
        assertThat(resultado).bodyJson().isStrictlyEqualTo("""
                {"errors":{"token":["is missing"]}}""");
    }

    @Test
    void deveDevolverUsuarioAtualComToken() {
        when(usuarioService.buscarAtual(7L)).thenReturn(JAKE);

        MvcTestResult resultado = mvc.get().uri("/api/user")
                .header(HttpHeaders.AUTHORIZATION, "Token " + tokenService.emitir(7L))
                .exchange();

        assertThat(resultado).hasStatus(200).hasContentType(JSON_UTF8);
        assertThat(resultado).bodyJson().isStrictlyEqualTo(USER_JAKE);
    }

    @Test
    void deveRecusarAtualizacaoSemCamposCom422() {
        MvcTestResult resultado = mvc.put().uri("/api/user").contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Token " + tokenService.emitir(7L))
                .content("""
                        {"user":{}}""")
                .exchange();

        assertThat(resultado).hasStatus(422).hasContentType(JSON_UTF8);
        assertThat(resultado).bodyJson().isStrictlyEqualTo("""
                {"errors":{"user":["can't be blank"]}}""");
        verifyNoInteractions(usuarioService);
    }

    @Test
    void deveTratarBioEImageVaziasComoNulas() {
        when(usuarioService.atualizar(any(), any())).thenReturn(JAKE);

        mvc.put().uri("/api/user").contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Token " + tokenService.emitir(7L))
                .content("""
                        {"user":{"bio":"","image":"   "}}""")
                .exchange();

        ArgumentCaptor<AtualizacaoUsuario> captor = ArgumentCaptor.forClass(AtualizacaoUsuario.class);
        verify(usuarioService).atualizar(eq(7L), captor.capture());
        assertThat(captor.getValue().bio()).isEqualTo(CampoAtualizado.de(null));
        assertThat(captor.getValue().imagem()).isEqualTo(CampoAtualizado.de(null));
    }
}
