package com.example.blog.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.example.blog.shared.error.ConflictException;
import com.example.blog.shared.error.UnprocessableException;
import com.example.blog.shared.security.TokenService;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final UserService userService = new UserService(userRepository, passwordEncoder, tokenService, CLOCK);

    @Test
    void deveRejeitarCadastroComUsernameJaUsado() {
        when(userRepository.existsByUsername("jake")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new Registration("jake", "jake@example.test", "senha")))
                .isInstanceOf(ConflictException.class)
                .extracting("key").isEqualTo("username");
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveNormalizarEmailAntesDeVerificarDuplicidadeNoCadastro() {
        when(userRepository.existsByEmail("jake@example.test")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new Registration("jake", "  Jake@Example.TEST ", "senha")))
                .isInstanceOf(ConflictException.class)
                .extracting("key").isEqualTo("email");
    }

    @Test
    void deveConferirSenhaMesmoQuandoEmailNaoExisteNoLogin() {
        when(userRepository.findByEmail("ninguem@example.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(new Credentials("ninguem@example.test", "senha")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(passwordEncoder).matches(anyString(), any());
        verify(tokenService, never()).issue(anyLong());
    }

    @Test
    void deveRejeitarLoginComSenhaIncorreta() {
        User user = new User("jake@example.test", "jake", "hash", CLOCK.instant());
        when(userRepository.findByEmail("jake@example.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(new Credentials("jake@example.test", "errada")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void deveRejeitarAtualizacaoSemCampos() {
        UserChanges noChanges = new UserChanges(null, null, null, false, null, false, null);

        assertThatThrownBy(() -> userService.update(1L, noChanges))
                .isInstanceOf(UnprocessableException.class);
        verify(userRepository, never()).findById(anyLong());
    }

}
