package com.example.blog.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.blog.profile.persistence.FollowId;
import com.example.blog.profile.persistence.FollowRepository;
import com.example.blog.shared.error.NotFoundException;
import com.example.blog.shared.error.ValidationException;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

class ProfileServiceTest {

    private static final long FOLLOWER_ID = 1L;
    private static final long CELEB_ID = 2L;

    private final UserRepository userRepository = mock(UserRepository.class);
    private final FollowRepository followRepository = mock(FollowRepository.class);
    private final ProfileService profileService = new ProfileService(userRepository, followRepository);

    @Test
    void deveSeguirUsuarioEMarcarFollowing() {
        when(userRepository.findByUsername("celeb")).thenReturn(Optional.of(user(CELEB_ID, "celeb")));

        ProfileData profile = profileService.follow(FOLLOWER_ID, "celeb");

        assertThat(profile.following()).isTrue();
        verify(followRepository).insertIfAbsent(FOLLOWER_ID, CELEB_ID);
    }

    @Test
    void deveRecusarSeguirASiMesmo() {
        when(userRepository.findByUsername("jake")).thenReturn(Optional.of(user(FOLLOWER_ID, "jake")));

        assertThatThrownBy(() -> profileService.follow(FOLLOWER_ID, "jake"))
                .isInstanceOf(ValidationException.class);
        verify(followRepository, never()).insertIfAbsent(anyLong(), anyLong());
    }

    @Test
    void deveLancarNaoEncontradoQuandoPerfilNaoExiste() {
        when(userRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile("ninguem"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deveInformarFollowingConformeRelacaoGravada() {
        when(userRepository.findByUsername("celeb")).thenReturn(Optional.of(user(CELEB_ID, "celeb")));
        when(followRepository.existsById(new FollowId(FOLLOWER_ID, CELEB_ID))).thenReturn(true);

        ProfileData profile = profileService.getProfile("celeb", FOLLOWER_ID);

        assertThat(profile.following()).isTrue();
    }

    private static User user(long id, String username) {
        User user = new User(username, username + "@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
