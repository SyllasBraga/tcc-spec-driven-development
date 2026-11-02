package com.example.blog.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.blog.shared.security.TokenService;
import com.example.blog.shared.security.UsuarioDoTokenInexistenteException;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Instant CRIACAO = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant AGORA = Instant.parse("2026-01-10T12:00:00Z");
    private static final String TAKEN = "has already been taken";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private UserService userService;

    @BeforeEach
    void configurar() {
        userService = new UserService(userRepository, passwordEncoder, tokenService,
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    // ---------- cadastrar ----------

    @Test
    void deveCadastrarNormalizandoEmailEUsername() {
        when(passwordEncoder.encode("jakejake")).thenReturn("{bcrypt}hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> comId(inv.getArgument(0), 7L));
        when(tokenService.emitir(7L)).thenReturn("tok");

        UsuarioAutenticado resultado = userService.cadastrar(new NovoUsuario(" jake ", " Jake@Jake.JAKE ", "jakejake"));

        ArgumentCaptor<User> gravado = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(gravado.capture());
        assertThat(gravado.getValue().getEmail()).isEqualTo("jake@jake.jake");
        assertThat(gravado.getValue().getUsername()).isEqualTo("jake");
        assertThat(gravado.getValue().getPasswordHash()).isEqualTo("{bcrypt}hash");
        assertThat(resultado).isEqualTo(new UsuarioAutenticado("jake@jake.jake", "tok", "jake", null, null));
    }

    @Test
    void deveRecusarCadastroComUsernameExistente() {
        when(userRepository.existsByUsername("jake")).thenReturn(true);

        assertThatThrownBy(() -> userService.cadastrar(new NovoUsuario("jake", "j@j.j", "x")))
                .isInstanceOfSatisfying(CampoJaUsadoException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.errors()).isEqualTo(Map.of("username", List.of(TAKEN)));
                });
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveRecusarCadastroComEmailExistente() {
        when(userRepository.existsByEmail("j@j.j")).thenReturn(true);

        assertThatThrownBy(() -> userService.cadastrar(new NovoUsuario("jake", "J@j.j", "x")))
                .isInstanceOfSatisfying(CampoJaUsadoException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.errors()).isEqualTo(Map.of("email", List.of(TAKEN)));
                });
    }

    @Test
    void deveDevolverOsDoisCamposQuandoAmbosJaExistem() {
        when(userRepository.existsByUsername("jake")).thenReturn(true);
        when(userRepository.existsByEmail("j@j.j")).thenReturn(true);

        assertThatThrownBy(() -> userService.cadastrar(new NovoUsuario("jake", "j@j.j", "x")))
                .isInstanceOfSatisfying(CampoJaUsadoException.class,
                        ex -> assertThat(ex.errors()).containsOnlyKeys("username", "email"));
    }

    // ---------- autenticar ----------

    @Test
    void deveAutenticarComEmailEmMaiusculas() {
        User jake = comId(new User("jake", "jake@jake.jake", "{bcrypt}hash", CRIACAO), 7L);
        when(userRepository.findByEmail("jake@jake.jake")).thenReturn(Optional.of(jake));
        when(passwordEncoder.matches("jakejake", "{bcrypt}hash")).thenReturn(true);
        when(tokenService.emitir(7L)).thenReturn("tok");

        UsuarioAutenticado resultado = userService.autenticar(new Credenciais("JAKE@jake.jake", "jakejake"));

        assertThat(resultado.token()).isEqualTo("tok");
        assertThat(resultado.username()).isEqualTo("jake");
    }

    @Test
    void deveRecusarSenhaErrada() {
        User jake = comId(new User("jake", "jake@jake.jake", "{bcrypt}hash", CRIACAO), 7L);
        when(userRepository.findByEmail("jake@jake.jake")).thenReturn(Optional.of(jake));
        when(passwordEncoder.matches("errada", "{bcrypt}hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.autenticar(new Credenciais("jake@jake.jake", "errada")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void deveRecusarEmailInexistente() {
        when(userRepository.findByEmail("ninguem@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.autenticar(new Credenciais("ninguem@x.com", "x")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    // ---------- buscarAtual ----------

    @Test
    void deveRecusarBuscaDeUsuarioInexistente() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.buscarAtual(99L))
                .isInstanceOf(UsuarioDoTokenInexistenteException.class);
    }

    @Test
    void deveBuscarUsuarioAtualComNovoToken() {
        User jake = comId(new User("jake", "jake@jake.jake", "{bcrypt}hash", CRIACAO), 7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(jake));
        when(tokenService.emitir(7L)).thenReturn("tok");

        assertThat(userService.buscarAtual(7L))
                .isEqualTo(new UsuarioAutenticado("jake@jake.jake", "tok", "jake", null, null));
    }

    // ---------- atualizar ----------

    @Test
    void deveRecusarAtualizacaoDeUsuarioInexistente() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.atualizar(99L, new AtualizacaoUsuario(null, null, null, "bio", null)))
                .isInstanceOf(UsuarioDoTokenInexistenteException.class);
    }

    @Test
    void deveAtualizarSomenteBioMantendoDemaisCampos() {
        User jake = jakeExistente();

        UsuarioAutenticado resultado = userService.atualizar(7L,
                new AtualizacaoUsuario(null, null, null, "I like to skateboard", null));

        assertThat(resultado).isEqualTo(
                new UsuarioAutenticado("jake@jake.jake", "tok", "jake", "I like to skateboard", "img"));
        assertThat(jake.getPasswordHash()).isEqualTo("{bcrypt}hash");
        assertThat(jake.getUpdatedAt()).isEqualTo(AGORA);
    }

    @Test
    void deveLimparBioQuandoVazia() {
        User jake = jakeExistente();

        userService.atualizar(7L, new AtualizacaoUsuario(null, null, null, "", null));

        assertThat(jake.getBio()).isNull();
        assertThat(jake.getImage()).isEqualTo("img");
    }

    @Test
    void deveLimparImagemQuandoSoEspacos() {
        User jake = jakeExistente();

        userService.atualizar(7L, new AtualizacaoUsuario(null, null, null, null, "  "));

        assertThat(jake.getImage()).isNull();
        assertThat(jake.getBio()).isEqualTo("bio");
    }

    @Test
    void deveRecusarAtualizacaoSemCampos() {
        assertThatThrownBy(() -> userService.atualizar(7L, new AtualizacaoUsuario(null, null, null, null, null)))
                .isInstanceOf(AtualizacaoVaziaException.class);
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void deveGravarHashDaNovaSenha() {
        User jake = jakeExistente();
        when(passwordEncoder.encode("nova")).thenReturn("{bcrypt}novo");

        userService.atualizar(7L, new AtualizacaoUsuario(null, null, "nova", null, null));

        assertThat(jake.getPasswordHash()).isEqualTo("{bcrypt}novo");
    }

    @Test
    void deveRecusarEmailDeOutroUsuarioComStatus422() {
        User jake = comId(new User("jake", "jake@jake.jake", "{bcrypt}hash", CRIACAO), 7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(jake));
        when(userRepository.existsByEmailAndIdNot("outro@x.com", 7L)).thenReturn(true);

        assertThatThrownBy(() -> userService.atualizar(7L,
                new AtualizacaoUsuario("Outro@x.com", null, null, null, null)))
                .isInstanceOfSatisfying(CampoJaUsadoException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
                    assertThat(ex.errors()).isEqualTo(Map.of("email", List.of(TAKEN)));
                });
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void naoDeveConsiderarConflitoTrocarParaOProprioUsername() {
        User jake = jakeExistente();
        when(userRepository.existsByUsernameAndIdNot("jake", 7L)).thenReturn(false);

        UsuarioAutenticado resultado = userService.atualizar(7L,
                new AtualizacaoUsuario(null, "jake", null, null, null));

        assertThat(resultado.username()).isEqualTo("jake");
        verify(userRepository, never()).existsByUsername(anyString());
        assertThat(jake.getUpdatedAt()).isEqualTo(AGORA);
    }

    private User jakeExistente() {
        User jake = comId(new User("jake", "jake@jake.jake", "{bcrypt}hash", CRIACAO), 7L);
        jake.alterarBio("bio");
        jake.alterarImagem("img");
        when(userRepository.findById(7L)).thenReturn(Optional.of(jake));
        when(userRepository.saveAndFlush(jake)).thenReturn(jake);
        when(tokenService.emitir(7L)).thenReturn("tok");
        return jake;
    }

    private static User comId(User user, long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
