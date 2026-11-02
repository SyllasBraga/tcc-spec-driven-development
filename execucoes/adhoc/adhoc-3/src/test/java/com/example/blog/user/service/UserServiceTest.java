package com.example.blog.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.example.blog.shared.error.ConflictException;
import com.example.blog.shared.error.UnauthorizedException;
import com.example.blog.shared.error.UnprocessableException;
import com.example.blog.user.persistence.AppUser;
import com.example.blog.user.persistence.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final long USER_ID = 7L;

    private final AppUserRepository repository = mock(AppUserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserService service =
            new UserService(repository, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void deveCadastrarComSenhaCodificadaECamposNormalizados() {
        when(passwordEncoder.encode("segredo1")).thenReturn("hash");
        when(repository.saveAndFlush(any(AppUser.class))).thenAnswer(call -> withId(call.getArgument(0)));

        UserData user = service.register(new Registration(" jake ", " jake@example.test ", "segredo1"));

        assertThat(user.username()).isEqualTo("jake");
        assertThat(user.email()).isEqualTo("jake@example.test");
        assertThat(user.bio()).isNull();
        verify(repository).saveAndFlush(any(AppUser.class));
    }

    @Test
    void deveRecusarCadastroComEmailJaUsado() {
        when(repository.existsByEmailIgnoringCase("jake@example.test")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new Registration("jake", "jake@example.test", "segredo1")))
                .isInstanceOf(ConflictException.class)
                .extracting("errorKey").isEqualTo("email");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRecusarCadastroComUsernameJaUsado() {
        when(repository.existsByUsername("jake")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new Registration("jake", "jake@example.test", "segredo1")))
                .isInstanceOf(ConflictException.class)
                .extracting("errorKey").isEqualTo("username");
    }

    @Test
    void deveRecusarSenhaAcimaDoLimiteDoBcrypt() {
        String longPassword = "a".repeat(UserService.MAX_PASSWORD_BYTES + 1);

        assertThatThrownBy(() -> service.register(new Registration("jake", "jake@example.test", longPassword)))
                .isInstanceOf(UnprocessableException.class)
                .extracting("errorKey").isEqualTo("password");
    }

    @Test
    void deveRecusarLoginComSenhaErrada() {
        AppUser user = withId(new AppUser("jake", "jake@example.test", "hash", NOW));
        when(repository.findByEmailIgnoringCase("jake@example.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login("jake@example.test", "errada"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deveRecusarLoginComEmailInexistente() {
        when(repository.findByEmailIgnoringCase("ninguem@example.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("ninguem@example.test", "segredo1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deveRecusarAtualizacaoSemCampos() {
        UserUpdate empty = new UserUpdate(null, null, null, false, null, false, null);

        assertThatThrownBy(() -> service.update(USER_ID, empty))
                .isInstanceOf(UnprocessableException.class)
                .extracting("errorKey").isEqualTo("user");
    }

    @Test
    void deveApagarImagemEnviadaComoNulaEManterBioAusente() {
        AppUser user = withId(new AppUser("jake", "jake@example.test", "hash", NOW));
        user.changeBio("bio antiga");
        user.changeImage("https://img.example.test/a.png");
        when(repository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(repository.saveAndFlush(user)).thenReturn(user);

        UserData updated = service.update(USER_ID, new UserUpdate(null, null, null, false, null, true, null));

        assertThat(updated.image()).isNull();
        assertThat(updated.bio()).isEqualTo("bio antiga");
    }

    @Test
    void deveGravarBioEmBrancoComoNula() {
        AppUser user = withId(new AppUser("jake", "jake@example.test", "hash", NOW));
        user.changeBio("bio antiga");
        when(repository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(repository.saveAndFlush(user)).thenReturn(user);

        UserData updated = service.update(USER_ID, new UserUpdate(null, null, null, true, "  ", true, ""));

        assertThat(updated.bio()).isNull();
        assertThat(updated.image()).isNull();
    }

    @Test
    void deveRecusarLoginComChaveCredentials() {
        when(repository.findByEmailIgnoringCase("ninguem@example.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("ninguem@example.test", "segredo1"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorKey").isEqualTo("credentials");
    }

    @Test
    void deveRecusarAtualizacaoParaEmailDeOutroUsuario() {
        AppUser user = withId(new AppUser("jake", "jake@example.test", "hash", NOW));
        when(repository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(repository.existsByEmailIgnoringCaseAndIdNot("outro@example.test", USER_ID)).thenReturn(true);

        UserUpdate update = new UserUpdate("outro@example.test", null, null, false, null, false, null);

        assertThatThrownBy(() -> service.update(USER_ID, update))
                .isInstanceOf(ConflictException.class)
                .extracting("errorKey").isEqualTo("email");
    }

    /** Simula o id atribuído pelo banco (coluna identity). */
    private static AppUser withId(AppUser user) {
        try {
            Field id = AppUser.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(user, USER_ID);
            return user;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Não foi possível definir o id de teste", e);
        }
    }
}
