package com.example.blog.perfil.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.perfil.persistence.Seguimento;
import com.example.blog.perfil.persistence.SeguimentoId;
import com.example.blog.perfil.persistence.SeguimentoRepository;
import com.example.blog.shared.erro.NaoAutenticadoException;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;

/**
 * Consulta de perfis públicos e relação de seguir. Seguir e deixar de seguir são idempotentes, e seguir a si
 * mesmo é permitido.
 */
@Service
public class PerfilService {

	private final UsuarioRepository usuarioRepository;

	private final SeguimentoRepository seguimentoRepository;

	private final Clock clock;

	public PerfilService(UsuarioRepository usuarioRepository, SeguimentoRepository seguimentoRepository,
			Clock clock) {
		this.usuarioRepository = usuarioRepository;
		this.seguimentoRepository = seguimentoRepository;
		this.clock = clock;
	}

	/**
	 * Consulta o perfil de um usuário.
	 *
	 * @param leitorId id de quem consulta, ou {@code null} em requisição anônima
	 * @throws PerfilNaoEncontradoException se o nome de usuário não existir
	 */
	@Transactional(readOnly = true)
	public Perfil consultar(String username, Long leitorId) {
		Usuario alvo = buscarAlvo(username);
		boolean following = leitorId != null
				&& seguimentoRepository.existsById(new SeguimentoId(leitorId, alvo.getId()));
		return perfil(alvo, following);
	}

	/**
	 * Faz {@code seguidorId} seguir o usuário {@code username}. Repetir a operação não cria outra relação.
	 *
	 * @throws PerfilNaoEncontradoException se o nome de usuário não existir
	 * @throws NaoAutenticadoException se o seguidor do token não existir mais
	 */
	@Transactional
	public Perfil seguir(String username, Long seguidorId) {
		Usuario alvo = buscarAlvo(username);
		exigirSeguidor(seguidorId);
		SeguimentoId id = new SeguimentoId(seguidorId, alvo.getId());
		if (!seguimentoRepository.existsById(id)) {
			seguimentoRepository.save(new Seguimento(id, clock.instant()));
		}
		return perfil(alvo, true);
	}

	/**
	 * Desfaz a relação de seguir, se ela existir.
	 *
	 * @throws PerfilNaoEncontradoException se o nome de usuário não existir
	 */
	@Transactional
	public Perfil deixarDeSeguir(String username, Long seguidorId) {
		Usuario alvo = buscarAlvo(username);
		SeguimentoId id = new SeguimentoId(seguidorId, alvo.getId());
		if (seguimentoRepository.existsById(id)) {
			seguimentoRepository.deleteById(id);
		}
		return perfil(alvo, false);
	}

	private Usuario buscarAlvo(String username) {
		return usuarioRepository.findByUsername(username).orElseThrow(PerfilNaoEncontradoException::new);
	}

	private void exigirSeguidor(Long seguidorId) {
		if (!usuarioRepository.existsById(seguidorId)) {
			throw NaoAutenticadoException.tokenInvalido();
		}
	}

	private static Perfil perfil(Usuario usuario, boolean following) {
		return new Perfil(usuario.getUsername(), usuario.getBio(), usuario.getImage(), following);
	}

}
