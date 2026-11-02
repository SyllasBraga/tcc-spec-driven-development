package com.example.blog.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.blog.shared.erro.ConflitoException;
import com.example.blog.shared.erro.CredenciaisInvalidasException;
import com.example.blog.shared.erro.RecursoNaoEncontradoException;
import com.example.blog.shared.erro.TokenInvalidoException;
import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-01T12:00:00Z");
    private static final Instant ANTES = Instant.parse("2026-10-01T12:00:00Z");
    private static final long ID = 7L;
    private static final String TOKEN = "token-ficticio";

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private UsuarioService service;

    @BeforeEach
    void configurar() {
        service = new UsuarioService(repository, passwordEncoder, tokenService, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    // cadastrar

    @Test
    void deveCadastrarGravandoHashEDevolvendoToken() {
        when(passwordEncoder.encode("senha1")).thenReturn("hash1");
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(chamada -> comId(chamada.getArgument(0), ID));
        when(tokenService.emitir(ID)).thenReturn(TOKEN);

        UsuarioAutenticado resultado = service.cadastrar("ana", "ana@exemplo.test", "senha1");

        ArgumentCaptor<Usuario> gravado = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).saveAndFlush(gravado.capture());
        assertThat(gravado.getValue().getSenhaHash()).isEqualTo("hash1");
        assertThat(gravado.getValue().getCriadoEm()).isEqualTo(AGORA);
        assertThat(resultado).isEqualTo(new UsuarioAutenticado("ana@exemplo.test", TOKEN, "ana", null, null));
    }

    @Test
    void deveRecusarCadastroComUsernameJaUsado() {
        when(repository.existsByUsername("ana")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar("ana", "ana@exemplo.test", "senha1"))
                .isInstanceOfSatisfying(ConflitoException.class, erro -> {
                    assertThat(erro.getChave()).isEqualTo("username");
                    assertThat(erro.getMensagem()).isEqualTo("has already been taken");
                });
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRecusarCadastroComEmailJaUsadoSemDiferenciarMaiusculas() {
        when(repository.existsByEmailIgnoreCase("ANA@exemplo.test")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar("ana", "ANA@exemplo.test", "senha1"))
                .isInstanceOfSatisfying(ConflitoException.class, erro -> {
                    assertThat(erro.getChave()).isEqualTo("email");
                    assertThat(erro.getMensagem()).isEqualTo("has already been taken");
                });
    }

    // autenticar

    @Test
    void deveAutenticarComCredenciaisCorretas() {
        Usuario usuario = comId(new Usuario("ana", "ana@exemplo.test", "hash1", ANTES), ID);
        when(repository.findByEmailIgnoreCase("Ana@Exemplo.test")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha1", "hash1")).thenReturn(true);
        when(tokenService.emitir(ID)).thenReturn(TOKEN);

        UsuarioAutenticado resultado = service.autenticar("Ana@Exemplo.test", "senha1");

        assertThat(resultado.token()).isEqualTo(TOKEN);
        assertThat(resultado.email()).isEqualTo("ana@exemplo.test");
    }

    @Test
    void deveRecusarLoginComEmailInexistente() {
        when(repository.findByEmailIgnoreCase("x@exemplo.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.autenticar("x@exemplo.test", "senha1"))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void deveRecusarLoginComSenhaErrada() {
        Usuario usuario = comId(new Usuario("ana", "ana@exemplo.test", "hash1", ANTES), ID);
        when(repository.findByEmailIgnoreCase("ana@exemplo.test")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "hash1")).thenReturn(false);

        assertThatThrownBy(() -> service.autenticar("ana@exemplo.test", "errada"))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    // buscarAtual

    @Test
    void deveBuscarUsuarioAtualDevolvendoOMesmoToken() {
        Usuario usuario = comId(new Usuario("ana", "ana@exemplo.test", "hash1", ANTES), ID);
        when(repository.findById(ID)).thenReturn(Optional.of(usuario));

        UsuarioAutenticado resultado = service.buscarAtual(ID, TOKEN);

        assertThat(resultado).isEqualTo(new UsuarioAutenticado("ana@exemplo.test", TOKEN, "ana", null, null));
    }

    @Test
    void deveLancarTokenInvalidoQuandoUsuarioAtualNaoExiste() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarAtual(ID, TOKEN)).isInstanceOf(TokenInvalidoException.class);
    }

    // atualizar

    @Test
    void deveAlterarSomenteOsCamposPresentes() {
        Usuario usuario = usuarioComBioEImagem();
        when(repository.findById(ID)).thenReturn(Optional.of(usuario));
        AlteracaoUsuario alteracao = new AlteracaoUsuario(AlteracaoUsuario.Campo.AUSENTE,
                AlteracaoUsuario.Campo.AUSENTE, AlteracaoUsuario.Campo.AUSENTE,
                AlteracaoUsuario.Campo.com("nova bio"), AlteracaoUsuario.Campo.AUSENTE);

        UsuarioAutenticado resultado = service.atualizar(ID, TOKEN, alteracao);

        assertThat(resultado).isEqualTo(
                new UsuarioAutenticado("ana@exemplo.test", TOKEN, "ana", "nova bio", "http://img.test/a.png"));
        assertThat(usuario.getAtualizadoEm()).isEqualTo(AGORA);
    }

    @Test
    void deveApagarBioEImagemQuandoPresentesComNulo() {
        Usuario usuario = usuarioComBioEImagem();
        when(repository.findById(ID)).thenReturn(Optional.of(usuario));
        AlteracaoUsuario alteracao = new AlteracaoUsuario(AlteracaoUsuario.Campo.AUSENTE,
                AlteracaoUsuario.Campo.AUSENTE, AlteracaoUsuario.Campo.AUSENTE,
                AlteracaoUsuario.Campo.com(null), AlteracaoUsuario.Campo.com(null));

        UsuarioAutenticado resultado = service.atualizar(ID, TOKEN, alteracao);

        assertThat(resultado.bio()).isNull();
        assertThat(resultado.image()).isNull();
    }

    @Test
    void deveGravarNovoHashAoTrocarSenha() {
        Usuario usuario = usuarioComBioEImagem();
        when(repository.findById(ID)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("nova")).thenReturn("hash-novo");
        AlteracaoUsuario alteracao = new AlteracaoUsuario(AlteracaoUsuario.Campo.AUSENTE,
                AlteracaoUsuario.Campo.AUSENTE, AlteracaoUsuario.Campo.com("nova"),
                AlteracaoUsuario.Campo.AUSENTE, AlteracaoUsuario.Campo.AUSENTE);

        service.atualizar(ID, TOKEN, alteracao);

        assertThat(usuario.getSenhaHash()).isEqualTo("hash-novo");
    }

    @Test
    void deveRecusarEmailDeOutroUsuarioNaAtualizacao() {
        when(repository.findById(ID)).thenReturn(Optional.of(usuarioComBioEImagem()));
        when(repository.existsByEmailIgnoreCaseAndIdNot("beto@exemplo.test", ID)).thenReturn(true);
        AlteracaoUsuario alteracao = new AlteracaoUsuario(AlteracaoUsuario.Campo.com("beto@exemplo.test"),
                AlteracaoUsuario.Campo.AUSENTE, AlteracaoUsuario.Campo.AUSENTE,
                AlteracaoUsuario.Campo.AUSENTE, AlteracaoUsuario.Campo.AUSENTE);

        assertThatThrownBy(() -> service.atualizar(ID, TOKEN, alteracao))
                .isInstanceOfSatisfying(ValidacaoException.class, erro -> {
                    assertThat(erro.getChave()).isEqualTo("email");
                    assertThat(erro.getMensagem()).isEqualTo("has already been taken");
                });
    }

    @Test
    void deveRecusarUsernameDeOutroUsuarioNaAtualizacao() {
        when(repository.findById(ID)).thenReturn(Optional.of(usuarioComBioEImagem()));
        when(repository.existsByUsernameAndIdNot("beto", ID)).thenReturn(true);
        AlteracaoUsuario alteracao = new AlteracaoUsuario(AlteracaoUsuario.Campo.AUSENTE,
                AlteracaoUsuario.Campo.com("beto"), AlteracaoUsuario.Campo.AUSENTE,
                AlteracaoUsuario.Campo.AUSENTE, AlteracaoUsuario.Campo.AUSENTE);

        assertThatThrownBy(() -> service.atualizar(ID, TOKEN, alteracao))
                .isInstanceOfSatisfying(ValidacaoException.class,
                        erro -> assertThat(erro.getChave()).isEqualTo("username"));
    }

    @Test
    void deveAceitarOProprioEmailEUsernameNaAtualizacao() {
        Usuario usuario = usuarioComBioEImagem();
        when(repository.findById(ID)).thenReturn(Optional.of(usuario));
        AlteracaoUsuario alteracao = new AlteracaoUsuario(AlteracaoUsuario.Campo.com("ANA@exemplo.test"),
                AlteracaoUsuario.Campo.com("ana"), AlteracaoUsuario.Campo.AUSENTE,
                AlteracaoUsuario.Campo.AUSENTE, AlteracaoUsuario.Campo.AUSENTE);

        UsuarioAutenticado resultado = service.atualizar(ID, TOKEN, alteracao);

        assertThat(resultado.email()).isEqualTo("ANA@exemplo.test");
        assertThat(resultado.username()).isEqualTo("ana");
    }

    @Test
    void deveLancarTokenInvalidoAoAtualizarUsuarioInexistente() {
        when(repository.findById(ID)).thenReturn(Optional.empty());
        AlteracaoUsuario alteracao = new AlteracaoUsuario(AlteracaoUsuario.Campo.AUSENTE,
                AlteracaoUsuario.Campo.AUSENTE, AlteracaoUsuario.Campo.AUSENTE,
                AlteracaoUsuario.Campo.com("bio"), AlteracaoUsuario.Campo.AUSENTE);

        assertThatThrownBy(() -> service.atualizar(ID, TOKEN, alteracao))
                .isInstanceOf(TokenInvalidoException.class);
    }

    // confirmarExistencia

    @Test
    void deveLancarTokenInvalidoQuandoUsuarioDoTokenNaoExiste() {
        when(repository.existsById(ID)).thenReturn(false);

        assertThatThrownBy(() -> service.confirmarExistencia(ID)).isInstanceOf(TokenInvalidoException.class);
    }

    // buscarPorUsername

    @Test
    void deveBuscarDadosPublicosPorUsername() {
        when(repository.findByUsername("ana")).thenReturn(Optional.of(usuarioComBioEImagem()));

        DadosPublicosUsuario dados = service.buscarPorUsername("ana");

        assertThat(dados).isEqualTo(new DadosPublicosUsuario(ID, "ana", "bio", "http://img.test/a.png"));
    }

    @Test
    void deveLancarNaoEncontradoQuandoUsernameNaoExiste() {
        when(repository.findByUsername("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorUsername("ninguem"))
                .isInstanceOfSatisfying(RecursoNaoEncontradoException.class, erro -> {
                    assertThat(erro.getChave()).isEqualTo("profile");
                    assertThat(erro.getMensagem()).isEqualTo("not found");
                });
    }

    private static Usuario usuarioComBioEImagem() {
        Usuario usuario = comId(new Usuario("ana", "ana@exemplo.test", "hash1", ANTES), ID);
        usuario.alterarBio("bio", ANTES);
        usuario.alterarImage("http://img.test/a.png", ANTES);
        return usuario;
    }

    private static Usuario comId(Usuario usuario, long id) {
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
