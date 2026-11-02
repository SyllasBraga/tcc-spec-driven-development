package com.example.blog.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.example.blog.shared.error.ConflictException;
import com.example.blog.shared.error.UnauthorizedException;
import com.example.blog.shared.error.UnprocessableException;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class UserServiceTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T12:00:00Z");

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(userRepository, passwordEncoder, Clock.fixed(AGORA, ZoneOffset.UTC));
        when(passwordEncoder.encode(any())).thenAnswer(invocation -> "hash:" + invocation.getArgument(0));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));
    }

    @Test
    void deveNormalizarEmailECodificarSenhaAoCadastrar() {
        UserData created = userService.register(" jake ", " Jake@Example.COM ", "jakejake");

        assertThat(created.username()).isEqualTo("jake");
        assertThat(created.email()).isEqualTo("jake@example.com");
        verify(passwordEncoder).encode("jakejake");
    }

    @Test
    void deveLancarConflitoQuandoEmailJaCadastrado() {
        when(userRepository.existsByEmail("jake@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("jake", "jake@example.com", "jakejake"))
                .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.field()).isEqualTo("email"));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarSenhaAcimaDe72Bytes() {
        assertThatThrownBy(() -> userService.register("jake", "jake@example.com", "á".repeat(37)))
                .isInstanceOfSatisfying(UnprocessableException.class, e -> assertThat(e.field()).isEqualTo("password"));
    }

    @Test
    void deveLancarNaoAutorizadoQuandoSenhaNaoConfere() {
        User user = withId(new User("jake", "jake@example.com", "hash", AGORA), 1L);
        when(userRepository.findByEmail("jake@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.login("JAKE@example.com", "errada"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deveRejeitarAtualizacaoSemCampos() {
        assertThatThrownBy(() -> userService.update(1L, new UserUpdate(null, null, null, null, null)))
                .isInstanceOf(UnprocessableException.class);
    }

    @Test
    void naoDeveVerificarUnicidadeQuandoUsernameNaoMuda() {
        User user = withId(new User("jake", "jake@example.com", "hash", AGORA), 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserData updated = userService.update(1L, new UserUpdate(null, "jake", null, "bio nova", ""));

        assertThat(updated.bio()).isEqualTo("bio nova");
        assertThat(updated.image()).isNull();
        verify(userRepository, never()).existsByUsername(any());
    }

    @Test
    void deveLancarConflitoQuandoNovoUsernamePertenceAOutroUsuario() {
        User user = withId(new User("jake", "jake@example.com", "hash", AGORA), 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("outro")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L, new UserUpdate(null, "outro", null, null, null)))
                .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.field()).isEqualTo("username"));
    }

    private static User withId(User user, long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
