package com.example.blog.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.blog.profile.persistence.FollowId;
import com.example.blog.profile.persistence.FollowRepository;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final long JAKE_ID = 1L;
    private static final long CELEB_ID = 2L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private ProfileService profileService;

    // ---------- buscar ----------

    @Test
    void deveLancarExcecaoQuandoPerfilNaoExiste() {
        when(userRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.buscar("ninguem", Optional.empty()))
                .isInstanceOf(PerfilNaoEncontradoException.class);
    }

    @Test
    void deveDevolverFollowingFalsoSemViewerSemConsultarRelacao() {
        existe("celeb", CELEB_ID);

        Perfil perfil = profileService.buscar("celeb", Optional.empty());

        assertThat(perfil).isEqualTo(new Perfil("celeb", null, null, false));
        verifyNoInteractions(followRepository);
    }

    @Test
    void deveDevolverFollowingVerdadeiroQuandoViewerSegue() {
        existe("celeb", CELEB_ID);
        when(followRepository.existsById(new FollowId(JAKE_ID, CELEB_ID))).thenReturn(true);

        assertThat(profileService.buscar("celeb", Optional.of(JAKE_ID)).following()).isTrue();
    }

    @Test
    void deveDevolverFollowingFalsoQuandoViewerNaoSegue() {
        existe("celeb", CELEB_ID);
        when(followRepository.existsById(new FollowId(JAKE_ID, CELEB_ID))).thenReturn(false);

        assertThat(profileService.buscar("celeb", Optional.of(JAKE_ID)).following()).isFalse();
    }

    // ---------- seguir / deixarDeSeguir ----------

    @Test
    void deveSeguirEDevolverFollowingVerdadeiro() {
        existe("celeb", CELEB_ID);

        Perfil perfil = profileService.seguir(JAKE_ID, "celeb");

        verify(followRepository).seguir(JAKE_ID, CELEB_ID);
        assertThat(perfil).isEqualTo(new Perfil("celeb", null, null, true));
    }

    @Test
    void deveRecusarSeguirASiMesmo() {
        existe("jake", JAKE_ID);

        assertThatThrownBy(() -> profileService.seguir(JAKE_ID, "jake"))
                .isInstanceOf(SeguirASiMesmoException.class);
        verify(followRepository, never()).seguir(anyLong(), anyLong());
    }

    @Test
    void deveLancarExcecaoAoSeguirPerfilInexistente() {
        when(userRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.seguir(JAKE_ID, "ninguem"))
                .isInstanceOf(PerfilNaoEncontradoException.class);
    }

    @Test
    void deveDeixarDeSeguirEDevolverFollowingFalso() {
        existe("celeb", CELEB_ID);

        Perfil perfil = profileService.deixarDeSeguir(JAKE_ID, "celeb");

        verify(followRepository).deixarDeSeguir(JAKE_ID, CELEB_ID);
        assertThat(perfil.following()).isFalse();
    }

    private void existe(String username, long id) {
        User user = new User(username, username + "@x.com", "{bcrypt}hash", Instant.EPOCH);
        ReflectionTestUtils.setField(user, "id", id);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    }
}
