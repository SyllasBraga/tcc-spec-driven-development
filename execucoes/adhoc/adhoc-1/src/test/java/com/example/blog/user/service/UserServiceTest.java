package com.example.blog.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.blog.shared.error.ConflictException;
import com.example.blog.shared.error.UnauthorizedException;
import com.example.blog.shared.error.ValidationException;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

class UserServiceTest {

    private static final long USER_ID = 1L;

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private UserService userService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hash:" + invocation.getArgument(0));
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void deveNormalizarEmailEGuardarHashDaSenhaNoCadastro() {
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        UserData user = userService.register(new Registration(" jake ", " Jake@Example.COM ", "jakejake"));

        assertThat(user.username()).isEqualTo("jake");
        assertThat(user.email()).isEqualTo("jake@example.com");
    }

    @Test
    void deveLancarConflitoQuandoNomeDeUsuarioJaExiste() {
        when(userRepository.existsByUsername("jake")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new Registration("jake", "jake@example.com", "jakejake")))
                .isInstanceOf(ConflictException.class)
                .extracting(exception -> ((ConflictException) exception).errors())
                .satisfies(errors -> assertThat(errors).containsOnlyKeys("username"));
    }

    @Test
    void deveRecusarLoginQuandoSenhaNaoConfere() {
        when(userRepository.findByEmail("jake@example.com")).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("errada", "hash:jakejake")).thenReturn(false);

        assertThatThrownBy(() -> userService.authenticate("jake@example.com", "errada"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deveRecusarLoginQuandoEmailNaoExiste() {
        when(userRepository.findByEmail("ninguem@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.authenticate("ninguem@example.com", "jakejake"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deveRecusarAtualizacaoSemCampos() {
        UserChanges semCampos = new UserChanges(null, null, null, null, null);

        assertThatThrownBy(() -> userService.update(USER_ID, semCampos))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void deveApagarBioQuandoEnviadaComoNula() {
        User user = existingUser();
        user.changeBio("bio antiga");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserData updated = userService.update(USER_ID, new UserChanges(null, null, null, new ValueChange(null), null));

        assertThat(updated.bio()).isNull();
    }

    @Test
    void deveRecusarAtualizacaoParaEmailDeOutroUsuarioSemAlterarEntidade() {
        User user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("outro@example.com", USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> userService.update(USER_ID,
                new UserChanges("Outro@Example.com", null, null, null, null)))
                .isInstanceOf(ValidationException.class);
        assertThat(user.getEmail()).isEqualTo("jake@example.com");
    }

    private static User existingUser() {
        return withId(new User("jake", "jake@example.com", "hash:jakejake"));
    }

    private static User withId(User user) {
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
