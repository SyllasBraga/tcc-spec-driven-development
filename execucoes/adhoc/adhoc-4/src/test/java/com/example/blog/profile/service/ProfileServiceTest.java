package com.example.blog.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.blog.profile.persistence.FollowRepository;
import com.example.blog.shared.error.ResourceNotFoundException;
import com.example.blog.shared.error.UnprocessableEntityException;
import com.example.blog.shared.security.AuthenticatedUser;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

class ProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final FollowRepository followRepository = mock(FollowRepository.class);
    private final ProfileService profileService =
            new ProfileService(userRepository, followRepository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void deveLancarNaoEncontradoParaUsernameInexistente() {
        when(userRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile("ninguem"))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("field").isEqualTo("profile");
    }

    @Test
    void deveImpedirSeguirASiMesmo() {
        when(userRepository.findByUsername("jake")).thenReturn(Optional.of(userWithId(1L, "jake")));

        assertThatThrownBy(() -> profileService.follow(new AuthenticatedUser(1L, "t"), "jake"))
                .isInstanceOf(UnprocessableEntityException.class);
        verify(followRepository, never()).insertIfAbsent(anyLong(), anyLong(), any());
    }

    @Test
    void deveSeguirUsuarioERetornarFollowingVerdadeiro() {
        when(userRepository.findByUsername("celeb")).thenReturn(Optional.of(userWithId(2L, "celeb")));
        when(userRepository.existsById(1L)).thenReturn(true);

        ProfileResult result = profileService.follow(new AuthenticatedUser(1L, "t"), "celeb");

        assertThat(result.following()).isTrue();
        verify(followRepository).insertIfAbsent(1L, 2L, NOW);
    }

    private static User userWithId(long id, String username) {
        User user = new User(username, username + "@exemplo.test", "hash", NOW);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
