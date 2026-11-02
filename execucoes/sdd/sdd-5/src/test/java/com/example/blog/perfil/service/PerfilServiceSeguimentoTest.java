package com.example.blog.perfil.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.blog.perfil.persistence.SeguimentoId;
import com.example.blog.perfil.persistence.SeguimentoRepository;
import com.example.blog.shared.erro.RecursoNaoEncontradoException;
import com.example.blog.shared.erro.TokenInvalidoException;
import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PerfilServiceSeguimentoTest {

    private static final Instant AGORA = Instant.parse("2026-11-01T12:00:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SeguimentoRepository seguimentoRepository;

    private PerfilService servico;
    private Usuario jake;

    @BeforeEach
    void preparar() {
        servico = new PerfilService(usuarioRepository, seguimentoRepository, Clock.fixed(AGORA, ZoneOffset.UTC));
        jake = PerfilServiceTest.comId(new Usuario("jake", "jake@jake.jake", "{bcrypt}hash", Instant.EPOCH), 1L);
    }

    @Test
    void deveSeguirDeFormaIdempotente() {
        when(usuarioRepository.findByUsername("jake")).thenReturn(Optional.of(jake));
        when(usuarioRepository.existsById(2L)).thenReturn(true);

        PerfilDto perfil = servico.seguir("jake", 2L);

        assertThat(perfil.seguindo()).isTrue();
        verify(seguimentoRepository).inserirSeNaoExistir(2L, 1L, AGORA);
    }

    @Test
    void deveImpedirSeguirASiMesmoSemGravar() {
        when(usuarioRepository.findByUsername("jake")).thenReturn(Optional.of(jake));
        when(usuarioRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> servico.seguir("jake", 1L))
                .isInstanceOfSatisfying(ValidacaoException.class, excecao -> assertThat(excecao.getErros())
                        .isEqualTo(Map.of("profile", List.of("cannot follow yourself"))));
        verifyNoInteractions(seguimentoRepository);
    }

    @Test
    void deveRemoverSomenteSeExistir() {
        when(usuarioRepository.findByUsername("jake")).thenReturn(Optional.of(jake));
        when(usuarioRepository.existsById(2L)).thenReturn(true);
        when(seguimentoRepository.existsById(new SeguimentoId(2L, 1L))).thenReturn(true);

        assertThat(servico.deixarDeSeguir("jake", 2L).seguindo()).isFalse();
        verify(seguimentoRepository).deleteById(new SeguimentoId(2L, 1L));
    }

    @Test
    void naoDeveRemoverQuandoNaoSegue() {
        when(usuarioRepository.findByUsername("jake")).thenReturn(Optional.of(jake));
        when(usuarioRepository.existsById(2L)).thenReturn(true);
        when(seguimentoRepository.existsById(new SeguimentoId(2L, 1L))).thenReturn(false);

        assertThat(servico.deixarDeSeguir("jake", 2L).seguindo()).isFalse();
        verify(seguimentoRepository, never()).deleteById(any());
    }

    @Test
    void deveLancarNaoEncontradoParaAlvoInexistente() {
        when(usuarioRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.seguir("ninguem", 2L)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.deixarDeSeguir("ninguem", 2L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveLancarTokenInvalidoQuandoAutenticadoNaoExiste() {
        when(usuarioRepository.findByUsername("jake")).thenReturn(Optional.of(jake));
        when(usuarioRepository.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> servico.seguir("jake", 2L)).isInstanceOf(TokenInvalidoException.class);
        verifyNoInteractions(seguimentoRepository);
    }
}
