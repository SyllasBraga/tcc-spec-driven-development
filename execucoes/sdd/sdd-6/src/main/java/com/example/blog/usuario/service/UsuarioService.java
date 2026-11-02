package com.example.blog.usuario.service;

import java.time.Clock;
import java.util.Locale;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.shared.erro.CampoJaUtilizadoException;
import com.example.blog.shared.erro.NaoAutenticadoException;
import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;

/**
 * Regras de cadastro, autenticação, consulta e atualização do usuário.
 * <p>
 * O e-mail é normalizado (sem espaços nas pontas e em minúsculas) e o nome de usuário é comparado exatamente,
 * depois de remover os espaços nas pontas. Senhas são gravadas apenas como hash.
 */
@Service
public class UsuarioService {

	private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

	private static final String CAMPO_EMAIL = "email";

	private static final String CAMPO_USERNAME = "username";

	private final UsuarioRepository usuarioRepository;

	private final PasswordEncoder passwordEncoder;

	private final TokenService tokenService;

	private final Clock clock;

	public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
			TokenService tokenService, Clock clock) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.clock = clock;
	}

	/**
	 * Cadastra um novo usuário.
	 *
	 * @throws CampoJaUtilizadoException se o e-mail ou o nome de usuário já estiverem em uso
	 */
	@Transactional
	public UsuarioAutenticado cadastrar(String username, String email, String senha) {
		String usernameNormalizado = username.trim();
		String emailNormalizado = normalizarEmail(email);
		if (usuarioRepository.existsByEmail(emailNormalizado)) {
			throw new CampoJaUtilizadoException(CAMPO_EMAIL);
		}
		if (usuarioRepository.existsByUsername(usernameNormalizado)) {
			throw new CampoJaUtilizadoException(CAMPO_USERNAME);
		}
		Usuario usuario = usuarioRepository.save(new Usuario(emailNormalizado, usernameNormalizado,
				passwordEncoder.encode(senha), clock.instant()));
		log.info("Usuário {} cadastrado", usuario.getId());
		return autenticado(usuario);
	}

	/**
	 * Autentica por e-mail e senha.
	 *
	 * @throws NaoAutenticadoException com {@code credentials: invalid} se o e-mail não existir ou a senha não
	 * conferir, sem indicar qual dos dois falhou
	 */
	@Transactional(readOnly = true)
	public UsuarioAutenticado autenticar(String email, String senha) {
		Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(email))
			.filter(encontrado -> passwordEncoder.matches(senha, encontrado.getSenhaHash()))
			.orElseThrow(NaoAutenticadoException::credenciaisInvalidas);
		return autenticado(usuario);
	}

	/**
	 * Consulta o usuário do token e emite um token novo.
	 *
	 * @throws NaoAutenticadoException com {@code token: is invalid} se o usuário não existir mais
	 */
	@Transactional(readOnly = true)
	public UsuarioAutenticado consultarAtual(Long id) {
		return autenticado(buscarAtual(id));
	}

	/**
	 * Aplica uma atualização parcial ao usuário do token. Campos {@link Alteracao.Manter} ficam inalterados.
	 *
	 * @throws NaoAutenticadoException se o usuário não existir mais
	 * @throws CampoJaUtilizadoException se o novo e-mail ou nome de usuário pertencer a outro usuário
	 */
	@Transactional
	public UsuarioAutenticado atualizar(Long id, AtualizacaoUsuario alteracao) {
		Usuario usuario = buscarAtual(id);
		aplicar(alteracao.email(), novoEmail -> {
			String emailNormalizado = normalizarEmail(novoEmail);
			if (usuarioRepository.existsByEmailAndIdNot(emailNormalizado, id)) {
				throw new CampoJaUtilizadoException(CAMPO_EMAIL);
			}
			usuario.alterarEmail(emailNormalizado);
		});
		aplicar(alteracao.username(), novoUsername -> {
			String usernameNormalizado = novoUsername.trim();
			if (usuarioRepository.existsByUsernameAndIdNot(usernameNormalizado, id)) {
				throw new CampoJaUtilizadoException(CAMPO_USERNAME);
			}
			usuario.alterarUsername(usernameNormalizado);
		});
		aplicar(alteracao.senha(), novaSenha -> usuario.alterarSenhaHash(passwordEncoder.encode(novaSenha)));
		aplicar(alteracao.bio(), usuario::alterarBio);
		aplicar(alteracao.image(), usuario::alterarImage);
		usuario.registrarAtualizacao(clock.instant());
		usuarioRepository.flush();
		log.info("Usuário {} atualizado", id);
		return autenticado(usuario);
	}

	private Usuario buscarAtual(Long id) {
		return usuarioRepository.findById(id).orElseThrow(NaoAutenticadoException::tokenInvalido);
	}

	private UsuarioAutenticado autenticado(Usuario usuario) {
		return new UsuarioAutenticado(usuario.getEmail(), tokenService.emitir(usuario.getId()),
				usuario.getUsername(), usuario.getBio(), usuario.getImage());
	}

	private static <T> void aplicar(Alteracao<T> alteracao, Consumer<T> acao) {
		switch (alteracao) {
			case Alteracao.Manter<T> _ -> {
			}
			case Alteracao.Definir<T>(T valor) -> acao.accept(valor);
		}
	}

	private static String normalizarEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
