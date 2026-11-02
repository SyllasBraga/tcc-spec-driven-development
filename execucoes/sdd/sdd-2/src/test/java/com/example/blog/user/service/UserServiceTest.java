package com.example.blog.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.blog.shared.error.ConflictException;
import com.example.blog.shared.error.InvalidCredentialsException;
import com.example.blog.shared.error.InvalidTokenException;
import com.example.blog.shared.error.ValidationException;
import com.example.blog.shared.security.TokenService;
import com.example.blog.user.persistence.UserEntity;
import com.example.blog.user.persistence.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Instant AGORA = Instant.parse("2026-01-10T12:00:00Z");
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Long ID = 7L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private UserService userService;

    @AfterAll
    static void fecharValidator() {
        VALIDATOR_FACTORY.close();
    }

    @BeforeEach
    void criarServico() {
        userService = new UserService(userRepository, passwordEncoder, tokenService,
                new EmailFormat(VALIDATOR_FACTORY.getValidator()), Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private static UserEntity usuarioPersistido(Long id, String username, String email, String hash) {
        UserEntity usuario = new UserEntity(username, email, hash, AGORA);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    @Nested
    class Cadastro {

        @Test
        void deveCadastrarUsuarioComSenhaCriptografadaEToken() {
            when(passwordEncoder.encode("jakejake")).thenReturn("hash-ficticio");
            when(userRepository.save(any(UserEntity.class))).thenAnswer(invocacao -> {
                UserEntity novo = invocacao.getArgument(0);
                ReflectionTestUtils.setField(novo, "id", ID);
                return novo;
            });
            when(tokenService.emitir(ID)).thenReturn("jwt-ficticio");

            AuthenticatedUser resultado = userService.cadastrar("Jacob", "jake@jake.jake", "jakejake");

            ArgumentCaptor<UserEntity> gravado = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(gravado.capture());
            assertThat(gravado.getValue().getPasswordHash()).isEqualTo("hash-ficticio");
            assertThat(gravado.getValue().getCreatedAt()).isEqualTo(AGORA);
            assertThat(resultado).isEqualTo(
                    new AuthenticatedUser(ID, "jake@jake.jake", "Jacob", null, null, "jwt-ficticio"));
        }

        @Test
        void deveRemoverEspacosDasPontasDeUsernameEEmail() {
            when(passwordEncoder.encode(anyString())).thenReturn("hash-ficticio");
            when(userRepository.save(any(UserEntity.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

            AuthenticatedUser resultado = userService.cadastrar("  Jacob ", " jake@jake.jake  ", "jakejake");

            verify(userRepository).existsByUsername("Jacob");
            verify(userRepository).existsByEmailIgnoreCase("jake@jake.jake");
            assertThat(resultado.username()).isEqualTo("Jacob");
            assertThat(resultado.email()).isEqualTo("jake@jake.jake");
        }

        @Test
        void deveLancarConflitoComUsernameEEmailQuandoAmbosJaExistem() {
            when(userRepository.existsByEmailIgnoreCase("jake@jake.jake")).thenReturn(true);
            when(userRepository.existsByUsername("Jacob")).thenReturn(true);

            assertThatThrownBy(() -> userService.cadastrar("Jacob", "jake@jake.jake", "jakejake"))
                    .isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.body().errors())
                            .containsOnlyKeys("email", "username")
                            .containsEntry("email", List.of("has already been taken")));
            verify(userRepository, never()).save(any());
        }

        @Test
        void deveLancarConflitoSoComEmailQuandoEmailJaExisteIgnorandoMaiusculas() {
            when(userRepository.existsByEmailIgnoreCase("JAKE@jake.jake")).thenReturn(true);

            assertThatThrownBy(() -> userService.cadastrar("Outro", "JAKE@jake.jake", "jakejake"))
                    .isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.body().errors())
                            .containsOnlyKeys("email"));
        }
    }

    @Nested
    class Autenticacao {

        @Test
        void deveAutenticarComEmailIgnorandoMaiusculas() {
            UserEntity usuario = usuarioPersistido(ID, "Jacob", "jake@jake.jake", "hash");
            when(userRepository.findByEmailIgnoreCase("JAKE@jake.jake")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("jakejake", "hash")).thenReturn(true);
            when(tokenService.emitir(ID)).thenReturn("jwt-ficticio");

            AuthenticatedUser resultado = userService.autenticar(" JAKE@jake.jake ", "jakejake");

            assertThat(resultado.email()).isEqualTo("jake@jake.jake");
            assertThat(resultado.token()).isEqualTo("jwt-ficticio");
        }

        @Test
        void deveLancarCredenciaisInvalidasQuandoEmailNaoExiste() {
            when(userRepository.findByEmailIgnoreCase("ninguem@x.y")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.autenticar("ninguem@x.y", "qualquer"))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        void deveLancarCredenciaisInvalidasQuandoSenhaNaoConfere() {
            UserEntity usuario = usuarioPersistido(ID, "Jacob", "jake@jake.jake", "hash");
            when(userRepository.findByEmailIgnoreCase("jake@jake.jake")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

            assertThatThrownBy(() -> userService.autenticar("jake@jake.jake", "errada"))
                    .isInstanceOf(InvalidCredentialsException.class);
            verify(tokenService, never()).emitir(anyLong());
        }
    }

    @Nested
    class UsuarioAtual {

        private UserEntity usuario;

        @BeforeEach
        void prepararUsuario() {
            usuario = usuarioPersistido(ID, "Jacob", "jake@jake.jake", "hash-antigo");
        }

        private UserUpdate somenteBio(String bio) {
            return new UserUpdate(false, null, false, null, false, null, true, bio, false, null);
        }

        @Test
        void deveBuscarUsuarioAtualComNovoToken() {
            when(userRepository.findById(ID)).thenReturn(Optional.of(usuario));
            when(tokenService.emitir(ID)).thenReturn("jwt-novo");

            AuthenticatedUser resultado = userService.buscarAtual(ID);

            assertThat(resultado.username()).isEqualTo("Jacob");
            assertThat(resultado.token()).isEqualTo("jwt-novo");
        }

        @Test
        void deveLancarTokenInvalidoQuandoUsuarioNaoExisteNaBusca() {
            when(userRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.buscarAtual(ID)).isInstanceOf(InvalidTokenException.class);
        }

        @Test
        void deveLancarTokenInvalidoQuandoUsuarioNaoExisteNaAtualizacao() {
            when(userRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.atualizar(ID, somenteBio("x")))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        void deveAtualizarSomenteOsCamposPresentes() {
            when(userRepository.findById(ID)).thenReturn(Optional.of(usuario));
            usuario.alterarImage("https://exemplo.invalid/antiga.png");
            UserUpdate update = new UserUpdate(false, null, false, null, false, null,
                    true, "I like to skateboard", false, null);

            AuthenticatedUser resultado = userService.atualizar(ID, update);

            assertThat(resultado.bio()).isEqualTo("I like to skateboard");
            assertThat(resultado.image()).isEqualTo("https://exemplo.invalid/antiga.png");
            assertThat(resultado.email()).isEqualTo("jake@jake.jake");
            assertThat(usuario.getUpdatedAt()).isEqualTo(AGORA);
        }

        @Test
        void deveLimparBioQuandoEnviadaComoNula() {
            when(userRepository.findById(ID)).thenReturn(Optional.of(usuario));
            usuario.alterarBio("antiga");

            AuthenticatedUser resultado = userService.atualizar(ID, somenteBio(null));

            assertThat(resultado.bio()).isNull();
        }

        @Test
        void deveLimparBioEImageQuandoEnviadasEmBranco() {
            when(userRepository.findById(ID)).thenReturn(Optional.of(usuario));
            usuario.alterarBio("antiga");
            usuario.alterarImage("https://exemplo.invalid/antiga.png");
            UserUpdate update = new UserUpdate(false, null, false, null, false, null, true, "", true, "   ");

            AuthenticatedUser resultado = userService.atualizar(ID, update);

            assertThat(resultado.bio()).isNull();
            assertThat(resultado.image()).isNull();
        }

        @Test
        void deveLancarValidacaoQuandoCamposObrigatoriosVemEmBrancoOuNulos() {
            UserUpdate update = new UserUpdate(true, "  ", true, null, true, "", false, null, false, null);

            assertThatThrownBy(() -> userService.atualizar(ID, update))
                    .isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.body().errors())
                            .isEqualTo(Map.of(
                                    "email", List.of("can't be blank"),
                                    "username", List.of("can't be blank"),
                                    "password", List.of("can't be blank"))));
        }

        @Test
        void deveLancarValidacaoQuandoNenhumCampoEnviado() {
            UserUpdate vazio = new UserUpdate(false, null, false, null, false, null, false, null, false, null);

            assertThatThrownBy(() -> userService.atualizar(ID, vazio))
                    .isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.body().errors())
                            .isEqualTo(Map.of("user", List.of("can't be empty"))));
        }

        @Test
        void deveLancarValidacaoQuandoEmailInvalido() {
            UserUpdate update = new UserUpdate(true, "nao-e-email", false, null, false, null, false, null, false, null);

            assertThatThrownBy(() -> userService.atualizar(ID, update))
                    .isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.body().errors())
                            .isEqualTo(Map.of("email", List.of("is invalid"))));
        }

        @Test
        void deveLancarConflitoQuandoEmailPertenceAOutroUsuario() {
            when(userRepository.findById(ID)).thenReturn(Optional.of(usuario));
            when(userRepository.existsByEmailIgnoreCaseAndIdNot("outro@x.y", ID)).thenReturn(true);
            UserUpdate update = new UserUpdate(true, "outro@x.y", false, null, false, null, false, null, false, null);

            assertThatThrownBy(() -> userService.atualizar(ID, update))
                    .isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.body().errors())
                            .containsOnlyKeys("email"));
        }

        @Test
        void deveAceitarOProprioEmailEUsernameSemConflito() {
            when(userRepository.findById(ID)).thenReturn(Optional.of(usuario));
            UserUpdate update = new UserUpdate(true, "JAKE@jake.jake", true, "Jacob", false, null,
                    false, null, false, null);

            AuthenticatedUser resultado = userService.atualizar(ID, update);

            verify(userRepository).existsByEmailIgnoreCaseAndIdNot("JAKE@jake.jake", ID);
            verify(userRepository).existsByUsernameAndIdNot("Jacob", ID);
            assertThat(resultado.email()).isEqualTo("JAKE@jake.jake");
        }

        @Test
        void deveGravarNovaSenhaComHash() {
            when(userRepository.findById(ID)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.encode("nova-senha")).thenReturn("hash-novo");
            UserUpdate update = new UserUpdate(false, null, false, null, true, "nova-senha",
                    false, null, false, null);

            userService.atualizar(ID, update);

            assertThat(usuario.getPasswordHash()).isEqualTo("hash-novo");
        }
    }
}
