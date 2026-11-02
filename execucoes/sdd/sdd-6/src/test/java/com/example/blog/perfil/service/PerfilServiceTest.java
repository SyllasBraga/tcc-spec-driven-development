package com.example.blog.perfil.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.blog.perfil.persistence.Seguimento;
import com.example.blog.perfil.persistence.SeguimentoId;
import com.example.blog.perfil.persistence.SeguimentoRepository;
import com.example.blog.shared.erro.RecursoNaoEncontradoException;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

	private static final Instant AGORA = Instant.parse("2026-11-02T12:00:00Z");

	private static final Long ID_ALVO = 2L;

	private static final Long ID_LEITOR = 1L;

	private static final SeguimentoId RELACAO = new SeguimentoId(ID_LEITOR, ID_ALVO);

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private SeguimentoRepository seguimentoRepository;

	private PerfilService service;

	@BeforeEach
	void configurar() {
		service = new PerfilService(usuarioRepository, seguimentoRepository, Clock.fixed(AGORA, ZoneOffset.UTC));
	}

	@Test
	void consultaAnonimaNaoSegueENaoConsultaRelacao() {
		comAlvo();

		Perfil perfil = service.consultar("jake", null);

		assertThat(perfil).isEqualTo(new Perfil("jake", "bio do jake", null, false));
		verifyNoInteractions(seguimentoRepository);
	}

	@Test
	void consultaDeQuemSegueDeveIndicarFollowing() {
		comAlvo();
		when(seguimentoRepository.existsById(RELACAO)).thenReturn(true);

		assertThat(service.consultar("jake", ID_LEITOR).following()).isTrue();
	}

	@Test
	void consultaDeUsernameInexistenteDeveFalhar() {
		when(usuarioRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.consultar("ninguem", null))
			.isInstanceOfSatisfying(PerfilNaoEncontradoException.class, excecao -> {
				assertThat(excecao).isInstanceOf(RecursoNaoEncontradoException.class);
				assertThat(excecao.getChave()).isEqualTo("profile");
				assertThat(excecao.getMensagem()).isEqualTo("not found");
			});
	}

	@Test
	void seguirDeveInserirRelacaoQuandoNaoExiste() {
		comAlvo();
		when(usuarioRepository.existsById(ID_LEITOR)).thenReturn(true);
		when(seguimentoRepository.existsById(RELACAO)).thenReturn(false);

		Perfil perfil = service.seguir("jake", ID_LEITOR);

		assertThat(perfil.following()).isTrue();
		verify(seguimentoRepository).save(new Seguimento(RELACAO, AGORA));
	}

	@Test
	void seguirNaoDeveInserirDeNovoQuandoJaExiste() {
		comAlvo();
		when(usuarioRepository.existsById(ID_LEITOR)).thenReturn(true);
		when(seguimentoRepository.existsById(RELACAO)).thenReturn(true);

		Perfil perfil = service.seguir("jake", ID_LEITOR);

		assertThat(perfil.following()).isTrue();
		verify(seguimentoRepository, never()).save(any());
	}

	@Test
	void seguirASiMesmoDeveSerPermitido() {
		comAlvo();
		when(usuarioRepository.existsById(ID_ALVO)).thenReturn(true);

		Perfil perfil = service.seguir("jake", ID_ALVO);

		assertThat(perfil.following()).isTrue();
		verify(seguimentoRepository).save(new Seguimento(new SeguimentoId(ID_ALVO, ID_ALVO), AGORA));
	}

	@Test
	void seguirAlvoInexistenteDeveFalhar() {
		when(usuarioRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.seguir("ninguem", ID_LEITOR))
			.isInstanceOf(PerfilNaoEncontradoException.class);
	}

	@Test
	void deixarDeSeguirDeveRemoverQuandoExiste() {
		comAlvo();
		when(seguimentoRepository.existsById(RELACAO)).thenReturn(true);

		Perfil perfil = service.deixarDeSeguir("jake", ID_LEITOR);

		assertThat(perfil.following()).isFalse();
		verify(seguimentoRepository).deleteById(RELACAO);
	}

	@Test
	void deixarDeSeguirNaoDeveFalharQuandoNaoExiste() {
		comAlvo();
		when(seguimentoRepository.existsById(RELACAO)).thenReturn(false);

		Perfil perfil = service.deixarDeSeguir("jake", ID_LEITOR);

		assertThat(perfil.following()).isFalse();
		verify(seguimentoRepository, never()).deleteById(any());
	}

	@Test
	void deixarDeSeguirAlvoInexistenteDeveFalhar() {
		when(usuarioRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deixarDeSeguir("ninguem", ID_LEITOR))
			.isInstanceOf(PerfilNaoEncontradoException.class);
	}

	private void comAlvo() {
		Usuario alvo = new Usuario("jake@example.com", "jake", "{bcrypt}hash", AGORA);
		alvo.alterarBio("bio do jake");
		ReflectionTestUtils.setField(alvo, "id", ID_ALVO);
		when(usuarioRepository.findByUsername("jake")).thenReturn(Optional.of(alvo));
	}

}
