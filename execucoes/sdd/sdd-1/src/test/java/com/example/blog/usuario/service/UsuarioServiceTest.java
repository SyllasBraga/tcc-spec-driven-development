package com.example.blog.usuario.service;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.blog.shared.security.TokenInvalidoException;
import com.example.blog.shared.security.TokenService;
import com.example.blog.shared.web.ConflitoException;
import com.example.blog.shared.web.CredenciaisInvalidasException;
import com.example.blog.shared.web.ValidacaoException;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;
import com.example.blog.usuario.service.AtualizacaoUsuario.Campo;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-31T12:00:00Z");
    private static final Long ID = 7L;
    private static final String TOKEN = "token-ficticio";
    private static final String SENHA = "senha-ficticia";

    private static final PasswordEncoder PASSWORD_ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TokenService tokenService;

    private UsuarioService usuarioService;

    @BeforeEach
    void configurar() {
        usuarioService = new UsuarioService(usuarioRepository, PASSWORD_ENCODER, tokenService,
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void deveRegistrarComEmailNormalizadoESenhaComHash() {
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocacao -> comId(invocacao.getArgument(0)));
        when(tokenService.emitir(ID)).thenReturn(TOKEN);

        UsuarioAutenticado resultado = usuarioService.registrar("  jake ", "  Jake@X.com ", SENHA);

        ArgumentCaptor<Usuario> gravado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(gravado.capture());
        assertThat(gravado.getValue().getEmail()).isEqualTo("jake@x.com");
        assertThat(gravado.getValue().getUsername()).isEqualTo("jake");
        assertThat(gravado.getValue().getSenhaHash()).startsWith("{bcrypt}").doesNotContain(SENHA);
        assertThat(resultado).isEqualTo(new UsuarioAutenticado("jake@x.com", TOKEN, "jake", null, null));
    }

    @Test
    void deveManterCaixaDoUsernameNoRegistro() {
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocacao -> comId(invocacao.getArgument(0)));

        UsuarioAutenticado resultado = usuarioService.registrar("Jake", "jake@x.com", SENHA);

        assertThat(resultado.username()).isEqualTo("Jake");
    }

    @Test
    void deveRecusarRegistroComEmailJaUsado() {
        when(usuarioRepository.existsByEmail("jake@x.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar("jake", "JAKE@x.com", SENHA))
                .isInstanceOfSatisfying(ConflitoException.class, ex -> {
                    assertThat(ex.getCampo()).isEqualTo("email");
                    assertThat(ex.getMensagem()).isEqualTo("has already been taken");
                });
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveRecusarRegistroComUsernameJaUsado() {
        when(usuarioRepository.existsByUsername("jake")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar("jake", "jake@x.com", SENHA))
                .isInstanceOfSatisfying(ConflitoException.class, ex -> assertThat(ex.getCampo()).isEqualTo("username"));
    }

    @Test
    void deveConverterViolacaoDeUnicidadeDoBancoEmConflitoNoRegistro() {
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uq_usuario_username\""));

        assertThatThrownBy(() -> usuarioService.registrar("jake", "jake@x.com", SENHA))
                .isInstanceOfSatisfying(ConflitoException.class, ex -> assertThat(ex.getCampo()).isEqualTo("username"));
    }

    @Test
    void deveEntrarComCredenciaisCorretasEEmailEmOutraCaixa() {
        Usuario usuario = usuarioPersistido();
        when(usuarioRepository.findByEmail("jake@x.com")).thenReturn(Optional.of(usuario));
        when(tokenService.emitir(ID)).thenReturn(TOKEN);

        UsuarioAutenticado resultado = usuarioService.entrar(" JAKE@x.com", SENHA);

        assertThat(resultado.token()).isEqualTo(TOKEN);
        assertThat(resultado.email()).isEqualTo("jake@x.com");
    }

    @Test
    void deveRecusarLoginComSenhaErrada() {
        when(usuarioRepository.findByEmail("jake@x.com")).thenReturn(Optional.of(usuarioPersistido()));

        assertThatThrownBy(() -> usuarioService.entrar("jake@x.com", "senha-errada"))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(tokenService, never()).emitir(anyLong());
    }

    @Test
    void deveRecusarLoginComEmailInexistenteComOMesmoErro() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.entrar("ninguem@x.com", SENHA))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void deveBuscarUsuarioAtualDevolvendoOMesmoToken() {
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuarioPersistido()));

        UsuarioAutenticado resultado = usuarioService.buscarAtual(ID, TOKEN);

        assertThat(resultado.token()).isEqualTo(TOKEN);
        assertThat(resultado.username()).isEqualTo("jake");
    }

    @Test
    void deveLancarTokenInvalidoQuandoUsuarioDoTokenNaoExiste() {
        when(usuarioRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarAtual(ID, TOKEN)).isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    void deveAtualizarSomenteBioPreservandoDemaisCampos() {
        Usuario usuario = usuarioPersistido();
        usuario.alterarImage("https://exemplo.test/img.png", AGORA.minusSeconds(60));
        prepararGravacao(usuario);

        UsuarioAutenticado resultado = usuarioService.atualizar(ID, TOKEN, comando(Campo.ausente(), Campo.ausente(),
                Campo.ausente(), Campo.informado("Nova bio"), Campo.ausente()));

        assertThat(resultado).isEqualTo(new UsuarioAutenticado(
                "jake@x.com", TOKEN, "jake", "Nova bio", "https://exemplo.test/img.png"));
        assertThat(usuario.getAtualizadoEm()).isEqualTo(AGORA);
    }

    @Test
    void deveLimparBioQuandoInformadaNula() {
        Usuario usuario = usuarioPersistido();
        usuario.alterarBio("bio antiga", AGORA);
        prepararGravacao(usuario);

        UsuarioAutenticado resultado = usuarioService.atualizar(ID, TOKEN, comando(Campo.ausente(), Campo.ausente(),
                Campo.ausente(), Campo.informado(null), Campo.ausente()));

        assertThat(resultado.bio()).isNull();
    }

    @Test
    void deveRegravarHashAoTrocarSenha() {
        Usuario usuario = usuarioPersistido();
        prepararGravacao(usuario);

        usuarioService.atualizar(ID, TOKEN, comando(Campo.ausente(), Campo.ausente(),
                Campo.informado("nova-senha-ficticia"), Campo.ausente(), Campo.ausente()));

        assertThat(PASSWORD_ENCODER.matches("nova-senha-ficticia", usuario.getSenhaHash())).isTrue();
        assertThat(PASSWORD_ENCODER.matches(SENHA, usuario.getSenhaHash())).isFalse();
    }

    @Test
    void deveNormalizarEmailNaAtualizacao() {
        Usuario usuario = usuarioPersistido();
        prepararGravacao(usuario);

        UsuarioAutenticado resultado = usuarioService.atualizar(ID, TOKEN, comando(Campo.informado(" Novo@X.com "),
                Campo.ausente(), Campo.ausente(), Campo.ausente(), Campo.ausente()));

        assertThat(resultado.email()).isEqualTo("novo@x.com");
    }

    @Test
    void deveRecusarAtualizacaoComEmailEUsernameDeOutroUsuarioCom422() {
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuarioPersistido()));
        when(usuarioRepository.existsByEmailAndIdNot("outro@x.com", ID)).thenReturn(true);
        when(usuarioRepository.existsByUsernameAndIdNot("outro", ID)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.atualizar(ID, TOKEN, comando(Campo.informado("outro@x.com"),
                Campo.informado("outro"), Campo.ausente(), Campo.ausente(), Campo.ausente())))
                .isInstanceOfSatisfying(ValidacaoException.class, ex -> assertThat(ex.getErros()).isEqualTo(Map.of(
                        "email", List.of("has already been taken"),
                        "username", List.of("has already been taken"))));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void devePermitirManterOProprioEmailEUsername() {
        Usuario usuario = usuarioPersistido();
        prepararGravacao(usuario);

        UsuarioAutenticado resultado = usuarioService.atualizar(ID, TOKEN, comando(Campo.informado("jake@x.com"),
                Campo.informado("jake"), Campo.ausente(), Campo.ausente(), Campo.ausente()));

        assertThat(resultado.username()).isEqualTo("jake");
    }

    @Test
    void deveConverterViolacaoDeUnicidadeDoBancoEm422NaAtualizacao() {
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuarioPersistido()));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uq_usuario_email\""));

        assertThatThrownBy(() -> usuarioService.atualizar(ID, TOKEN, comando(Campo.informado("novo@x.com"),
                Campo.ausente(), Campo.ausente(), Campo.ausente(), Campo.ausente())))
                .isInstanceOfSatisfying(ValidacaoException.class,
                        ex -> assertThat(ex.getErros()).containsOnlyKeys("email"));
    }

    @Test
    void deveLancarTokenInvalidoAoAtualizarUsuarioInexistente() {
        when(usuarioRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.atualizar(ID, TOKEN, comando(Campo.ausente(), Campo.ausente(),
                Campo.ausente(), Campo.informado("bio"), Campo.ausente())))
                .isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    void deveBuscarDadosPublicosPorUsername() {
        when(usuarioRepository.findByUsername("jake")).thenReturn(Optional.of(usuarioPersistido()));

        assertThat(usuarioService.buscarPublicoPorUsername("jake"))
                .contains(new UsuarioPublico(ID, "jake", null, null));
    }

    private void prepararGravacao(Usuario usuario) {
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);
    }

    private static AtualizacaoUsuario comando(Campo email, Campo username, Campo password, Campo bio, Campo image) {
        return new AtualizacaoUsuario(email, username, password, bio, image);
    }

    private static Usuario usuarioPersistido() {
        return comId(new Usuario("jake@x.com", "jake", PASSWORD_ENCODER.encode(SENHA), AGORA.minusSeconds(3600)));
    }

    private static Usuario comId(Usuario usuario) {
        ReflectionTestUtils.setField(usuario, "id", ID);
        return usuario;
    }
}
