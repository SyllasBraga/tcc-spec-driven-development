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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.blog.shared.erro.CampoJaUtilizadoException;
import com.example.blog.shared.erro.NaoAutenticadoException;
import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	private static final Instant AGORA = Instant.parse("2026-11-02T12:00:00Z");

	private static final Long ID = 1L;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private TokenService tokenService;

	private UsuarioService service;

	@BeforeEach
	void configurar() {
		service = new UsuarioService(usuarioRepository, passwordEncoder, tokenService,
				Clock.fixed(AGORA, ZoneOffset.UTC));
	}

	@Test
	void cadastrarDeveNormalizarEmailEGravarHash() {
		when(passwordEncoder.encode("segredo")).thenReturn("{bcrypt}hash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
		when(tokenService.emitir(any())).thenReturn("jwt");

		UsuarioAutenticado resultado = service.cadastrar(" jake ", "  Jake@Example.COM ", "segredo");

		ArgumentCaptor<Usuario> gravado = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(gravado.capture());
		assertThat(gravado.getValue().getEmail()).isEqualTo("jake@example.com");
		assertThat(gravado.getValue().getUsername()).isEqualTo("jake");
		assertThat(gravado.getValue().getSenhaHash()).isEqualTo("{bcrypt}hash");
		assertThat(resultado).isEqualTo(new UsuarioAutenticado("jake@example.com", "jwt", "jake", null, null));
	}

	@Test
	void cadastrarDeveRecusarEmailExistente() {
		when(usuarioRepository.existsByEmail("jake@example.com")).thenReturn(true);

		assertThatThrownBy(() -> service.cadastrar("jake", "jake@example.com", "segredo"))
			.isInstanceOfSatisfying(CampoJaUtilizadoException.class,
					excecao -> assertThat(excecao.getChave()).isEqualTo("email"));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void cadastrarDeveRecusarUsernameExistente() {
		when(usuarioRepository.existsByUsername("jake")).thenReturn(true);

		assertThatThrownBy(() -> service.cadastrar("jake", "jake@example.com", "segredo"))
			.isInstanceOfSatisfying(CampoJaUtilizadoException.class,
					excecao -> assertThat(excecao.getChave()).isEqualTo("username"));
	}

	@Test
	void autenticarDeveRecusarEmailInexistente() {
		when(usuarioRepository.findByEmail("ninguem@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.autenticar("ninguem@example.com", "segredo"))
			.isInstanceOfSatisfying(NaoAutenticadoException.class, excecao -> {
				assertThat(excecao.getChave()).isEqualTo("credentials");
				assertThat(excecao.getMensagem()).isEqualTo("invalid");
			});
	}

	@Test
	void autenticarDeveRecusarSenhaErrada() {
		when(usuarioRepository.findByEmail("jake@example.com")).thenReturn(Optional.of(usuario()));
		when(passwordEncoder.matches("errada", "{bcrypt}hash")).thenReturn(false);

		assertThatThrownBy(() -> service.autenticar("jake@example.com", "errada"))
			.isInstanceOfSatisfying(NaoAutenticadoException.class,
					excecao -> assertThat(excecao.getChave()).isEqualTo("credentials"));
	}

	@Test
	void autenticarDeveDevolverTokenQuandoCredenciaisConferem() {
		when(usuarioRepository.findByEmail("jake@example.com")).thenReturn(Optional.of(usuario()));
		when(passwordEncoder.matches("segredo", "{bcrypt}hash")).thenReturn(true);
		when(tokenService.emitir(any())).thenReturn("jwt");

		UsuarioAutenticado resultado = service.autenticar(" JAKE@example.com", "segredo");

		assertThat(resultado.token()).isEqualTo("jwt");
		assertThat(resultado.username()).isEqualTo("jake");
	}

	@Test
	void consultarAtualDeveRecusarUsuarioInexistente() {
		when(usuarioRepository.findById(ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.consultarAtual(ID))
			.isInstanceOfSatisfying(NaoAutenticadoException.class, excecao -> {
				assertThat(excecao.getChave()).isEqualTo("token");
				assertThat(excecao.getMensagem()).isEqualTo("is invalid");
			});
	}

	@Test
	void atualizarDeveAlterarSomenteCamposDefinidos() {
		Usuario usuario = usuario();
		when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));
		when(tokenService.emitir(any())).thenReturn("novo-jwt");

		UsuarioAutenticado resultado = service.atualizar(ID, new AtualizacaoUsuario(Alteracao.manter(),
				Alteracao.manter(), Alteracao.manter(), Alteracao.definir("I like to skateboard"),
				Alteracao.definir("https://example.com/foto.jpg")));

		assertThat(resultado).isEqualTo(new UsuarioAutenticado("jake@example.com", "novo-jwt", "jake",
				"I like to skateboard", "https://example.com/foto.jpg"));
		verify(passwordEncoder, never()).encode(anyString());
		verify(usuarioRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
	}

	@Test
	void atualizarDeveRecusarEmailDeOutroUsuario() {
		when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario()));
		when(usuarioRepository.existsByEmailAndIdNot("outro@example.com", ID)).thenReturn(true);

		assertThatThrownBy(() -> service.atualizar(ID, apenasEmail("Outro@Example.com")))
			.isInstanceOfSatisfying(CampoJaUtilizadoException.class,
					excecao -> assertThat(excecao.getChave()).isEqualTo("email"));
	}

	@Test
	void atualizarDeveAceitarOProprioEmailAtual() {
		Usuario usuario = usuario();
		when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));
		when(usuarioRepository.existsByEmailAndIdNot("jake@example.com", ID)).thenReturn(false);
		when(tokenService.emitir(any())).thenReturn("jwt");

		UsuarioAutenticado resultado = service.atualizar(ID, apenasEmail("jake@example.com"));

		assertThat(resultado.email()).isEqualTo("jake@example.com");
	}

	@Test
	void atualizarDeveRecusarUsernameDeOutroUsuario() {
		when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario()));
		when(usuarioRepository.existsByUsernameAndIdNot("outro", ID)).thenReturn(true);

		assertThatThrownBy(() -> service.atualizar(ID, new AtualizacaoUsuario(Alteracao.manter(),
				Alteracao.definir("outro"), Alteracao.manter(), Alteracao.manter(), Alteracao.manter())))
			.isInstanceOfSatisfying(CampoJaUtilizadoException.class,
					excecao -> assertThat(excecao.getChave()).isEqualTo("username"));
	}

	@Test
	void atualizarDeveGravarNovaSenhaComoHash() {
		Usuario usuario = usuario();
		when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));
		when(passwordEncoder.encode("nova")).thenReturn("{bcrypt}novo");

		service.atualizar(ID, new AtualizacaoUsuario(Alteracao.manter(), Alteracao.manter(),
				Alteracao.definir("nova"), Alteracao.manter(), Alteracao.manter()));

		assertThat(usuario.getSenhaHash()).isEqualTo("{bcrypt}novo");
	}

	@Test
	void atualizarComBioNulaDeveLimparOCampo() {
		Usuario usuario = usuario();
		usuario.alterarBio("antiga");
		when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));

		service.atualizar(ID, new AtualizacaoUsuario(Alteracao.manter(), Alteracao.manter(), Alteracao.manter(),
				Alteracao.definir(null), Alteracao.manter()));

		assertThat(usuario.getBio()).isNull();
	}

	private static AtualizacaoUsuario apenasEmail(String email) {
		return new AtualizacaoUsuario(Alteracao.definir(email), Alteracao.manter(), Alteracao.manter(),
				Alteracao.manter(), Alteracao.manter());
	}

	private static Usuario usuario() {
		return new Usuario("jake@example.com", "jake", "{bcrypt}hash", AGORA);
	}

}
