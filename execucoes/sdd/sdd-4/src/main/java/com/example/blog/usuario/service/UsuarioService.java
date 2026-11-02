package com.example.blog.usuario.service;

import static com.example.blog.shared.erro.MensagensDeErro.JA_EM_USO;
import static com.example.blog.shared.erro.MensagensDeErro.NAO_ENCONTRADO;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.shared.erro.ConflitoException;
import com.example.blog.shared.erro.CredenciaisInvalidasException;
import com.example.blog.shared.erro.ErroDeNegocioException;
import com.example.blog.shared.erro.RecursoNaoEncontradoException;
import com.example.blog.shared.erro.TokenInvalidoException;
import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;

/**
 * Regras de negócio da conta de usuário: cadastro, login, consulta e atualização do usuário atual e consulta
 * dos dados públicos usados no perfil.
 */
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private static final String CAMPO_USERNAME = "username";
    private static final String CAMPO_EMAIL = "email";
    private static final String CHAVE_PERFIL = "profile";
    private static final String RESTRICAO_USERNAME = "uq_usuario_username";
    private static final String RESTRICAO_EMAIL = "uq_usuario_email_lower";

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder, TokenService tokenService,
            Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    /**
     * Cadastra um novo usuário e emite um token para ele.
     *
     * @throws ConflitoException com chave {@code username} ou {@code email} se o valor já pertencer a outro
     *         usuário (o e-mail é comparado sem diferenciar maiúsculas)
     */
    @Transactional
    public UsuarioAutenticado cadastrar(String username, String email, String senha) {
        if (repository.existsByUsername(username)) {
            throw new ConflitoException(CAMPO_USERNAME, JA_EM_USO);
        }
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException(CAMPO_EMAIL, JA_EM_USO);
        }
        Usuario usuario = new Usuario(username, email, passwordEncoder.encode(senha), Instant.now(clock));
        Usuario salvo = gravarTratandoUnicidade(() -> repository.saveAndFlush(usuario), ConflitoException::new);
        log.info("Usuário {} cadastrado", salvo.getId());
        return autenticado(salvo, tokenService.emitir(salvo.getId()));
    }

    /**
     * Confere e-mail (sem diferenciar maiúsculas) e senha e emite um novo token.
     *
     * @throws CredenciaisInvalidasException se o e-mail não existir ou a senha não conferir
     */
    @Transactional(readOnly = true)
    public UsuarioAutenticado autenticar(String email, String senha) {
        Usuario usuario = repository.findByEmailIgnoreCase(email)
                .filter(encontrado -> passwordEncoder.matches(senha, encontrado.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);
        return autenticado(usuario, tokenService.emitir(usuario.getId()));
    }

    /**
     * Devolve os dados do usuário dono do token.
     *
     * @param token token recebido na requisição, devolvido sem alteração
     * @throws TokenInvalidoException se o usuário não existir mais
     */
    @Transactional(readOnly = true)
    public UsuarioAutenticado buscarAtual(long id, String token) {
        return autenticado(buscarDonoDoToken(id), token);
    }

    /**
     * Altera somente os campos presentes em {@code alteracao}. A nova senha é gravada como hash.
     *
     * @param token token recebido na requisição, devolvido sem alteração
     * @throws ValidacaoException com chave {@code username} ou {@code email} se o novo valor já pertencer a
     *         outro usuário
     * @throws TokenInvalidoException se o usuário não existir mais
     */
    @Transactional
    public UsuarioAutenticado atualizar(long id, String token, AlteracaoUsuario alteracao) {
        Usuario usuario = buscarDonoDoToken(id);
        Instant agora = Instant.now(clock);
        if (alteracao.username().presente()) {
            String novoUsername = alteracao.username().valor();
            if (repository.existsByUsernameAndIdNot(novoUsername, id)) {
                throw new ValidacaoException(CAMPO_USERNAME, JA_EM_USO);
            }
            usuario.alterarUsername(novoUsername, agora);
        }
        if (alteracao.email().presente()) {
            String novoEmail = alteracao.email().valor();
            if (repository.existsByEmailIgnoreCaseAndIdNot(novoEmail, id)) {
                throw new ValidacaoException(CAMPO_EMAIL, JA_EM_USO);
            }
            usuario.alterarEmail(novoEmail, agora);
        }
        if (alteracao.password().presente()) {
            usuario.alterarSenhaHash(passwordEncoder.encode(alteracao.password().valor()), agora);
        }
        if (alteracao.bio().presente()) {
            usuario.alterarBio(alteracao.bio().valor(), agora);
        }
        if (alteracao.image().presente()) {
            usuario.alterarImage(alteracao.image().valor(), agora);
        }
        gravarTratandoUnicidade(() -> {
            repository.flush();
            return usuario;
        }, ValidacaoException::new);
        return autenticado(usuario, token);
    }

    /**
     * Busca os dados públicos de um usuário pelo nome (comparação exata).
     *
     * @throws RecursoNaoEncontradoException com chave {@code profile} se o usuário não existir
     */
    @Transactional(readOnly = true)
    public DadosPublicosUsuario buscarPorUsername(String username) {
        Usuario usuario = repository.findByUsername(username)
                .orElseThrow(() -> new RecursoNaoEncontradoException(CHAVE_PERFIL, NAO_ENCONTRADO));
        return new DadosPublicosUsuario(usuario.getId(), usuario.getUsername(), usuario.getBio(), usuario.getImage());
    }

    /**
     * Confirma que o usuário identificado pelo token ainda existe.
     *
     * @throws TokenInvalidoException se o usuário não existir mais
     */
    @Transactional(readOnly = true)
    public void confirmarExistencia(long id) {
        if (!repository.existsById(id)) {
            throw new TokenInvalidoException();
        }
    }

    private Usuario buscarDonoDoToken(long id) {
        return repository.findById(id).orElseThrow(TokenInvalidoException::new);
    }

    /**
     * Executa a gravação convertendo violação das restrições únicas, que só ocorre em corrida entre requisições
     * simultâneas, no mesmo erro que a verificação prévia produziria.
     */
    private static Usuario gravarTratandoUnicidade(Supplier<Usuario> gravacao,
            BiFunction<String, String, ErroDeNegocioException> construtorDeErro) {
        try {
            return gravacao.get();
        } catch (DataIntegrityViolationException excecao) {
            Optional<String> campo = campoDaRestricaoViolada(excecao);
            if (campo.isEmpty()) {
                throw excecao;
            }
            ErroDeNegocioException erro = construtorDeErro.apply(campo.get(), JA_EM_USO);
            erro.initCause(excecao);
            throw erro;
        }
    }

    private static Optional<String> campoDaRestricaoViolada(DataIntegrityViolationException excecao) {
        String mensagem = String.valueOf(excecao.getMostSpecificCause().getMessage());
        if (mensagem.contains(RESTRICAO_USERNAME)) {
            return Optional.of(CAMPO_USERNAME);
        }
        if (mensagem.contains(RESTRICAO_EMAIL)) {
            return Optional.of(CAMPO_EMAIL);
        }
        return Optional.empty();
    }

    private static UsuarioAutenticado autenticado(Usuario usuario, String token) {
        return new UsuarioAutenticado(usuario.getEmail(), token, usuario.getUsername(), usuario.getBio(),
                usuario.getImage());
    }

}
