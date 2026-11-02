package com.example.blog.perfil.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.blog.perfil.persistence.SeguimentoId;
import com.example.blog.perfil.persistence.SeguimentoRepository;
import com.example.blog.shared.erro.RecursoNaoEncontradoException;
import com.example.blog.shared.erro.TokenInvalidoException;
import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.usuario.service.DadosPublicosUsuario;
import com.example.blog.usuario.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-01T12:00:00Z");
    private static final long ID_ATUAL = 1L;
    private static final long ID_ALVO = 2L;
    private static final DadosPublicosUsuario BETO = new DadosPublicosUsuario(ID_ALVO, "beto", "bio do beto", null);

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private SeguimentoRepository seguimentoRepository;

    private PerfilService service;

    @BeforeEach
    void configurar() {
        service = new PerfilService(usuarioService, seguimentoRepository, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    // buscar / buscarComo

    @Test
    void deveDevolverFollowingFalsoParaAnonimo() {
        when(usuarioService.buscarPorUsername("beto")).thenReturn(BETO);

        assertThat(service.buscar("beto")).isEqualTo(new Perfil("beto", "bio do beto", null, false));
        verifyNoInteractions(seguimentoRepository);
    }

    @Test
    void deveDevolverFollowingVerdadeiroQuandoUsuarioAtualSegue() {
        when(usuarioService.buscarPorUsername("beto")).thenReturn(BETO);
        when(seguimentoRepository.existsById(new SeguimentoId(ID_ATUAL, ID_ALVO))).thenReturn(true);

        assertThat(service.buscarComo(ID_ATUAL, "beto").following()).isTrue();
    }

    @Test
    void deveDevolverFollowingFalsoQuandoUsuarioAtualNaoSegue() {
        when(usuarioService.buscarPorUsername("beto")).thenReturn(BETO);
        when(seguimentoRepository.existsById(new SeguimentoId(ID_ATUAL, ID_ALVO))).thenReturn(false);

        assertThat(service.buscarComo(ID_ATUAL, "beto").following()).isFalse();
    }

    @Test
    void devePropagarNaoEncontradoQuandoUsernameNaoExiste() {
        when(usuarioService.buscarPorUsername("ninguem"))
                .thenThrow(new RecursoNaoEncontradoException("profile", "not found"));

        assertThatThrownBy(() -> service.buscar("ninguem")).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveRecusarConsultaAutenticadaDeUsuarioQueNaoExisteMais() {
        doThrow(new TokenInvalidoException()).when(usuarioService).confirmarExistencia(ID_ATUAL);

        assertThatThrownBy(() -> service.buscarComo(ID_ATUAL, "beto")).isInstanceOf(TokenInvalidoException.class);
    }

    // seguir

    @Test
    void deveSeguirEDevolverFollowingVerdadeiro() {
        when(usuarioService.buscarPorUsername("beto")).thenReturn(BETO);

        Perfil perfil = service.seguir(ID_ATUAL, "beto");

        verify(seguimentoRepository).seguir(ID_ATUAL, ID_ALVO, AGORA);
        assertThat(perfil).isEqualTo(new Perfil("beto", "bio do beto", null, true));
    }

    @Test
    void deveDevolverFollowingVerdadeiroQuandoJaSegue() {
        when(usuarioService.buscarPorUsername("beto")).thenReturn(BETO);
        when(seguimentoRepository.seguir(ID_ATUAL, ID_ALVO, AGORA)).thenReturn(0);

        assertThat(service.seguir(ID_ATUAL, "beto").following()).isTrue();
    }

    @Test
    void deveRecusarSeguirASiMesmo() {
        when(usuarioService.buscarPorUsername("ana")).thenReturn(new DadosPublicosUsuario(ID_ATUAL, "ana", null, null));

        assertThatThrownBy(() -> service.seguir(ID_ATUAL, "ana"))
                .isInstanceOfSatisfying(ValidacaoException.class, erro -> {
                    assertThat(erro.getChave()).isEqualTo("profile");
                    assertThat(erro.getMensagem()).isEqualTo("cannot follow yourself");
                });
        verify(seguimentoRepository, never()).seguir(anyLong(), anyLong(), any());
    }

    @Test
    void deveRecusarSeguirAlvoInexistente() {
        when(usuarioService.buscarPorUsername("ninguem"))
                .thenThrow(new RecursoNaoEncontradoException("profile", "not found"));

        assertThatThrownBy(() -> service.seguir(ID_ATUAL, "ninguem"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // deixarDeSeguir

    @Test
    void deveDeixarDeSeguirEDevolverFollowingFalso() {
        when(usuarioService.buscarPorUsername("beto")).thenReturn(BETO);
        when(seguimentoRepository.deixarDeSeguir(ID_ATUAL, ID_ALVO)).thenReturn(1);

        Perfil perfil = service.deixarDeSeguir(ID_ATUAL, "beto");

        assertThat(perfil.following()).isFalse();
    }

    @Test
    void deveDeixarDeSeguirSemErroQuandoNaoSegue() {
        when(usuarioService.buscarPorUsername("beto")).thenReturn(BETO);
        when(seguimentoRepository.deixarDeSeguir(ID_ATUAL, ID_ALVO)).thenReturn(0);

        assertThat(service.deixarDeSeguir(ID_ATUAL, "beto").following()).isFalse();
    }

    @Test
    void deveRecusarDeixarDeSeguirAlvoInexistente() {
        when(usuarioService.buscarPorUsername("ninguem"))
                .thenThrow(new RecursoNaoEncontradoException("profile", "not found"));

        assertThatThrownBy(() -> service.deixarDeSeguir(ID_ATUAL, "ninguem"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
