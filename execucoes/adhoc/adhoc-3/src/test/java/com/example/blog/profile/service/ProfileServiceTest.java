package com.example.blog.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;

import com.example.blog.profile.persistence.FollowId;
import com.example.blog.profile.persistence.FollowRepository;
import com.example.blog.shared.error.NotFoundException;
import com.example.blog.shared.error.UnprocessableException;
import com.example.blog.user.persistence.AppUser;
import com.example.blog.user.persistence.AppUserRepository;
import org.junit.jupiter.api.Test;

class ProfileServiceTest {

    private static final long TARGET_ID = 2L;
    private static final long VIEWER_ID = 1L;

    private final AppUserRepository userRepository = mock(AppUserRepository.class);
    private final FollowRepository followRepository = mock(FollowRepository.class);
    private final ProfileService service = new ProfileService(userRepository, followRepository);

    @Test
    void deveRetornarPerfilNaoSeguidoParaVisitanteAnonimo() {
        givenTarget("celeb");

        ProfileData profile = service.getProfile("celeb", null);

        assertThat(profile.username()).isEqualTo("celeb");
        assertThat(profile.following()).isFalse();
        verifyNoInteractions(followRepository);
    }

    @Test
    void deveIndicarQuandoVisitanteSegueOPerfil() {
        givenTarget("celeb");
        when(followRepository.existsById(new FollowId(VIEWER_ID, TARGET_ID))).thenReturn(true);

        assertThat(service.getProfile("celeb", VIEWER_ID).following()).isTrue();
    }

    @Test
    void deveLancarNaoEncontradoParaUsernameInexistente() {
        when(userRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.follow(VIEWER_ID, "ninguem"))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorKey").isEqualTo("profile");
    }

    @Test
    void deveSeguirUsuario() {
        givenTarget("celeb");

        ProfileData profile = service.follow(VIEWER_ID, "celeb");

        assertThat(profile.following()).isTrue();
        verify(followRepository).insertIfAbsent(VIEWER_ID, TARGET_ID);
    }

    @Test
    void deveRecusarSeguirASiMesmo() {
        givenTarget("celeb");

        assertThatThrownBy(() -> service.follow(TARGET_ID, "celeb")).isInstanceOf(UnprocessableException.class);
        verify(followRepository, never()).insertIfAbsent(anyLong(), anyLong());
    }

    @Test
    void deveDeixarDeSeguirUsuario() {
        givenTarget("celeb");

        ProfileData profile = service.unfollow(VIEWER_ID, "celeb");

        assertThat(profile.following()).isFalse();
        verify(followRepository).delete(VIEWER_ID, TARGET_ID);
    }

    private void givenTarget(String username) {
        AppUser user = new AppUser(username, username + "@example.test", "hash", Instant.EPOCH);
        try {
            Field id = AppUser.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(user, TARGET_ID);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Não foi possível definir o id de teste", e);
        }
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    }
}
