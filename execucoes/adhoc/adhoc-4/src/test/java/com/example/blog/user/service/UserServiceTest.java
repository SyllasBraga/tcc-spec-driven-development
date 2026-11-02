package com.example.blog.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.blog.shared.error.ConflictException;
import com.example.blog.shared.error.UnauthorizedException;
import com.example.blog.shared.error.UnprocessableEntityException;
import com.example.blog.shared.security.AuthenticatedUser;
import com.example.blog.shared.security.TokenService;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

class UserServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final TokenService tokenService = mock(TokenService.class);
    private UserService userService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenAnswer(call -> "hash:" + call.getArgument(0));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenAnswer(call -> ("hash:" + call.getArgument(0)).equals(call.getArgument(1)));
        userService = new UserService(userRepository, passwordEncoder, tokenService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void deveNormalizarEmailAoCadastrar() {
        when(tokenService.issueToken(1L)).thenReturn("token");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(call -> {
            User saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        UserResult result = userService.register(new RegisterUserCommand(" jake ", " Jake@Jake.JAKE ", "jakejake"));

        assertThat(result.email()).isEqualTo("jake@jake.jake");
        assertThat(result.username()).isEqualTo("jake");
        assertThat(result.token()).isEqualTo("token");
    }

    @Test
    void deveLancarConflitoQuandoUsernameJaExiste() {
        when(userRepository.existsByUsername("jake")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new RegisterUserCommand("jake", "a@b.c", "senha")))
                .isInstanceOf(ConflictException.class)
                .extracting("field").isEqualTo("username");
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarSenhaMaiorQue72Bytes() {
        String senhaLonga = "a".repeat(UserService.MAX_PASSWORD_BYTES + 1);

        assertThatThrownBy(() -> userService.register(new RegisterUserCommand("jake", "a@b.c", senhaLonga)))
                .isInstanceOf(UnprocessableEntityException.class)
                .extracting("field").isEqualTo("password");
    }

    @Test
    void deveLancarNaoAutorizadoQuandoSenhaNaoConfere() {
        User user = new User("jake", "jake@jake.jake", "hash:certa", NOW);
        when(userRepository.findByEmail("jake@jake.jake")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login("jake@jake.jake", "errada"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("field").isEqualTo("credentials");
    }

    @Test
    void deveLancarNaoAutorizadoQuandoEmailNaoExiste() {
        when(userRepository.findByEmail("ninguem@jake.jake")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login("ninguem@jake.jake", "qualquer"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deveRejeitarAtualizacaoSemCampos() {
        UpdateUserCommand vazio = new UpdateUserCommand(null, null, null, null, null);

        assertThatThrownBy(() -> userService.updateCurrentUser(new AuthenticatedUser(1L, "t"), vazio))
                .isInstanceOf(UnprocessableEntityException.class)
                .extracting("field").isEqualTo("user");
    }

    @Test
    void deveRejeitarAtualizacaoParaEmailDeOutroUsuario() {
        User user = new User("jake", "jake@jake.jake", "hash:x", NOW);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("outro@jake.jake")).thenReturn(true);
        UpdateUserCommand command = new UpdateUserCommand("outro@jake.jake", null, null, null, null);

        assertThatThrownBy(() -> userService.updateCurrentUser(new AuthenticatedUser(1L, "t"), command))
                .isInstanceOf(UnprocessableEntityException.class)
                .extracting("field").isEqualTo("email");
    }

    @Test
    void deveRemoverImagemQuandoTextoVazioEManterDemaisCampos() {
        User user = new User("jake", "jake@jake.jake", "hash:x", NOW);
        user.changeImage("https://exemplo.test/a.png", NOW);
        user.changeBio("bio", NOW);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UpdateUserCommand command = new UpdateUserCommand(null, null, null, null, "");

        UserResult result = userService.updateCurrentUser(new AuthenticatedUser(1L, "t"), command);

        assertThat(result.image()).isNull();
        assertThat(result.bio()).isEqualTo("bio");
        assertThat(result.token()).isEqualTo("t");
    }

    @Test
    void deveLancarNaoAutorizadoQuandoUsuarioDoTokenNaoExiste() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(new AuthenticatedUser(99L, "t")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
