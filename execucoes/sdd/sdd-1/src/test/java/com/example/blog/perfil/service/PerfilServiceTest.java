package com.example.blog.perfil.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.blog.perfil.persistence.Seguimento;
import com.example.blog.perfil.persistence.SeguimentoId;
import com.example.blog.perfil.persistence.SeguimentoRepository;
import com.example.blog.shared.web.RecursoNaoEncontradoException;
import com.example.blog.shared.web.ValidacaoException;
import com.example.blog.usuario.service.UsuarioPublico;
import com.example.blog.usuario.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    private static final Long ID_ANA = 1L;
    private static final Long ID_BIA = 2L;
    private static final UsuarioPublico BIA = new UsuarioPublico(ID_BIA, "bia", "bio da bia", null);
    private static final SeguimentoId ANA_SEGUE_BIA = new SeguimentoId(ID_ANA, ID_BIA);

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private SeguimentoRepository seguimentoRepository;

    private PerfilService perfilService;

    @BeforeEach
    void configurar() {
        perfilService = new PerfilService(usuarioService, seguimentoRepository,
                Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void deveConsultarAnonimamenteComFollowingFalso() {
        when(usuarioService.buscarPublicoPorUsername("bia")).thenReturn(Optional.of(BIA));

        assertThat(perfilService.consultar("bia")).isEqualTo(new Perfil("bia", "bio da bia", null, false));
    }

    @Test
    void deveInformarFollowingQuandoConsultaAutenticadaSegue() {
        when(usuarioService.buscarPublicoPorUsername("bia")).thenReturn(Optional.of(BIA));
        when(seguimentoRepository.existsById(ANA_SEGUE_BIA)).thenReturn(true);

        assertThat(perfilService.consultar("bia", ID_ANA).isFollowing()).isTrue();
    }

    @Test
    void deveLancarNaoEncontradoParaUsernameInexistente() {
        when(usuarioService.buscarPublicoPorUsername("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> perfilService.consultar("ninguem"))
                .isInstanceOfSatisfying(RecursoNaoEncontradoException.class,
                        ex -> assertThat(ex.getRecurso()).isEqualTo("profile"));
    }

    @Test
    void deveSeguirGravandoQuandoAindaNaoSegue() {
        when(usuarioService.buscarPublicoPorUsername("bia")).thenReturn(Optional.of(BIA));
        when(seguimentoRepository.existsById(ANA_SEGUE_BIA)).thenReturn(false);

        Perfil perfil = perfilService.seguir(ID_ANA, "bia");

        assertThat(perfil.isFollowing()).isTrue();
        verify(seguimentoRepository).save(any(Seguimento.class));
    }

    @Test
    void deveSeguirDeFormaIdempotente() {
        when(usuarioService.buscarPublicoPorUsername("bia")).thenReturn(Optional.of(BIA));
        when(seguimentoRepository.existsById(ANA_SEGUE_BIA)).thenReturn(true);

        Perfil perfil = perfilService.seguir(ID_ANA, "bia");

        assertThat(perfil.isFollowing()).isTrue();
        verify(seguimentoRepository, never()).save(any());
    }

    @Test
    void deveRecusarSeguirASiMesmo() {
        when(usuarioService.buscarPublicoPorUsername("bia")).thenReturn(Optional.of(BIA));

        assertThatThrownBy(() -> perfilService.seguir(ID_BIA, "bia"))
                .isInstanceOfSatisfying(ValidacaoException.class,
                        ex -> assertThat(ex.getErros()).containsEntry("profile",
                                List.of("cannot follow yourself")));
        verify(seguimentoRepository, never()).save(any());
    }

    @Test
    void deveDeixarDeSeguirRemovendoQuandoSegue() {
        when(usuarioService.buscarPublicoPorUsername("bia")).thenReturn(Optional.of(BIA));
        when(seguimentoRepository.existsById(ANA_SEGUE_BIA)).thenReturn(true);

        Perfil perfil = perfilService.deixarDeSeguir(ID_ANA, "bia");

        assertThat(perfil.isFollowing()).isFalse();
        verify(seguimentoRepository).deleteById(ANA_SEGUE_BIA);
    }

    @Test
    void deveDeixarDeSeguirDeFormaIdempotente() {
        when(usuarioService.buscarPublicoPorUsername("bia")).thenReturn(Optional.of(BIA));
        when(seguimentoRepository.existsById(ANA_SEGUE_BIA)).thenReturn(false);

        assertThat(perfilService.deixarDeSeguir(ID_ANA, "bia").isFollowing()).isFalse();
        verify(seguimentoRepository, never()).deleteById(any());
    }

    @Test
    void deveLancarNaoEncontradoAoSeguirOuDeixarDeSeguirInexistente() {
        when(usuarioService.buscarPublicoPorUsername("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> perfilService.seguir(ID_ANA, "ninguem"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> perfilService.deixarDeSeguir(ID_ANA, "ninguem"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
