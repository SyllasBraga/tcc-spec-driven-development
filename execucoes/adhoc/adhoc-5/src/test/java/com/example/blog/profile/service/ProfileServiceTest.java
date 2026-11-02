package com.example.blog.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.example.blog.profile.persistence.FollowId;
import com.example.blog.profile.persistence.FollowRepository;
import com.example.blog.shared.error.NotFoundException;
import com.example.blog.shared.error.UnprocessableException;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProfileServiceTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T12:00:00Z");
    private static final long SEGUIDOR_ID = 1L;
    private static final long CELEBRIDADE_ID = 2L;

    private UserRepository userRepository;
    private FollowRepository followRepository;
    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        followRepository = mock(FollowRepository.class);
        profileService = new ProfileService(userRepository, followRepository, Clock.fixed(AGORA, ZoneOffset.UTC));
        User celebridade = new User("celeb", "celeb@example.com", "hash", AGORA);
        ReflectionTestUtils.setField(celebridade, "id", CELEBRIDADE_ID);
        when(userRepository.findByUsername("celeb")).thenReturn(Optional.of(celebridade));
    }

    @Test
    void deveRetornarFollowingFalsoParaVisitanteAnonimo() {
        ProfileData profile = profileService.getProfile("celeb");

        assertThat(profile.following()).isFalse();
        verify(followRepository, never()).existsById(any());
    }

    @Test
    void deveRefletirRelacaoDoUsuarioAutenticado() {
        when(followRepository.existsById(new FollowId(SEGUIDOR_ID, CELEBRIDADE_ID))).thenReturn(true);

        assertThat(profileService.getProfile("celeb", SEGUIDOR_ID).following()).isTrue();
    }

    @Test
    void deveLancarNaoEncontradoQuandoPerfilNaoExiste() {
        when(userRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.follow(SEGUIDOR_ID, "ninguem"))
                .isInstanceOfSatisfying(NotFoundException.class, e -> assertThat(e.field()).isEqualTo("profile"));
    }

    @Test
    void deveRegistrarRelacaoAoSeguir() {
        ProfileData profile = profileService.follow(SEGUIDOR_ID, "celeb");

        assertThat(profile.following()).isTrue();
        verify(followRepository).insertIgnoringDuplicate(SEGUIDOR_ID, CELEBRIDADE_ID, AGORA);
    }

    @Test
    void deveRejeitarSeguirASiMesmo() {
        assertThatThrownBy(() -> profileService.follow(CELEBRIDADE_ID, "celeb"))
                .isInstanceOf(UnprocessableException.class);
        verify(followRepository, never()).insertIgnoringDuplicate(anyLong(), anyLong(), any());
    }

    @Test
    void deveRemoverRelacaoAoDeixarDeSeguir() {
        ProfileData profile = profileService.unfollow(SEGUIDOR_ID, "celeb");

        assertThat(profile.following()).isFalse();
        verify(followRepository).deleteFollow(new FollowId(SEGUIDOR_ID, CELEBRIDADE_ID));
    }
}
