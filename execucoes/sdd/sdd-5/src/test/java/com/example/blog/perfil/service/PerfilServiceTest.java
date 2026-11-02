package com.example.blog.perfil.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.blog.perfil.persistence.SeguimentoId;
import com.example.blog.perfil.persistence.SeguimentoRepository;
import com.example.blog.shared.erro.RecursoNaoEncontradoException;
import com.example.blog.shared.erro.TokenInvalidoException;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SeguimentoRepository seguimentoRepository;

    private PerfilService servico;
    private Usuario jake;

    @BeforeEach
    void preparar() {
        servico = new PerfilService(usuarioRepository, seguimentoRepository, Clock.systemUTC());
        jake = comId(new Usuario("jake", "jake@jake.jake", "{bcrypt}hash", Instant.EPOCH), 1L);
        jake.alterarBio("bio");
    }

    @Test
    void deveDevolverNaoSeguindoParaAnonimoSemConsultarSeguimentos() {
        when(usuarioRepository.findByUsername("jake")).thenReturn(Optional.of(jake));

        PerfilDto perfil = servico.buscarPerfil("jake", null);

        assertThat(perfil).isEqualTo(new PerfilDto("jake", "bio", null, false));
        verifyNoInteractions(seguimentoRepository);
    }

    @Test
    void deveDevolverSeguindoQuandoAutenticadoSegue() {
        when(usuarioRepository.findByUsername("jake")).thenReturn(Optional.of(jake));
        when(usuarioRepository.existsById(2L)).thenReturn(true);
        when(seguimentoRepository.existsById(new SeguimentoId(2L, 1L))).thenReturn(true);

        assertThat(servico.buscarPerfil("jake", 2L).seguindo()).isTrue();
    }

    @Test
    void deveDevolverNaoSeguindoQuandoAutenticadoNaoSegue() {
        when(usuarioRepository.findByUsername("jake")).thenReturn(Optional.of(jake));
        when(usuarioRepository.existsById(2L)).thenReturn(true);
        when(seguimentoRepository.existsById(new SeguimentoId(2L, 1L))).thenReturn(false);

        assertThat(servico.buscarPerfil("jake", 2L).seguindo()).isFalse();
    }

    @Test
    void deveLancarNaoEncontradoComChaveProfile() {
        when(usuarioRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.buscarPerfil("ninguem", null))
                .isInstanceOfSatisfying(RecursoNaoEncontradoException.class,
                        excecao -> assertThat(excecao.getChave()).isEqualTo("profile"));
    }

    @Test
    void deveLancarTokenInvalidoQuandoAutenticadoNaoExiste() {
        when(usuarioRepository.findByUsername("jake")).thenReturn(Optional.of(jake));
        when(usuarioRepository.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> servico.buscarPerfil("jake", 2L)).isInstanceOf(TokenInvalidoException.class);
    }

    static Usuario comId(Usuario usuario, Long id) {
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
