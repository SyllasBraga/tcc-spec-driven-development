package com.example.blog.user.service;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.shared.security.TokenService;
import com.example.blog.shared.security.UsuarioDoTokenInexistenteException;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

/**
 * Cadastro, login, consulta e atualização do usuário.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final String RESTRICAO_USERNAME = "uq_users_username";
    private static final String RESTRICAO_EMAIL = "uq_users_email";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    /**
     * Cadastra um usuário com e-mail normalizado e senha em hash.
     *
     * @throws CampoJaUsadoException (409) se o nome de usuário e/ou o e-mail já estiverem em uso
     */
    @Transactional
    public UsuarioAutenticado cadastrar(NovoUsuario novo) {
        String username = normalizarUsername(novo.username());
        String email = normalizarEmail(novo.email());

        Set<String> conflitos = new LinkedHashSet<>();
        if (userRepository.existsByUsername(username)) {
            conflitos.add(CampoJaUsadoException.USERNAME);
        }
        if (userRepository.existsByEmail(email)) {
            conflitos.add(CampoJaUsadoException.EMAIL);
        }
        if (!conflitos.isEmpty()) {
            throw CampoJaUsadoException.noCadastro(conflitos);
        }

        var user = new User(username, email, passwordEncoder.encode(novo.password()), clock.instant());
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw CampoJaUsadoException.noCadastro(camposDaViolacao(e));
        }
        log.info("Usuário {} cadastrado", user.getId());
        return autenticado(user);
    }

    /**
     * Confere e-mail (sem diferenciar maiúsculas) e senha.
     *
     * @throws CredenciaisInvalidasException se o e-mail não existir ou a senha não conferir
     */
    @Transactional(readOnly = true)
    public UsuarioAutenticado autenticar(Credenciais credenciais) {
        User user = userRepository.findByEmail(normalizarEmail(credenciais.email()))
                .filter(encontrado -> passwordEncoder.matches(credenciais.password(), encontrado.getPasswordHash()))
                .orElseThrow(CredenciaisInvalidasException::new);
        return autenticado(user);
    }

    /**
     * @throws UsuarioDoTokenInexistenteException se o id do token não existir mais
     */
    @Transactional(readOnly = true)
    public UsuarioAutenticado buscarAtual(long id) {
        return autenticado(carregar(id));
    }

    /**
     * Aplica somente os campos informados.
     *
     * @throws AtualizacaoVaziaException se nenhum campo for informado
     * @throws UsuarioDoTokenInexistenteException se o id do token não existir mais
     * @throws CampoJaUsadoException (422) se o novo nome de usuário ou e-mail pertencer a outra pessoa
     */
    @Transactional
    public UsuarioAutenticado atualizar(long id, AtualizacaoUsuario atualizacao) {
        if (atualizacao.isVazia()) {
            throw new AtualizacaoVaziaException();
        }
        User user = carregar(id);
        String email = atualizacao.email() == null ? null : normalizarEmail(atualizacao.email());
        String username = atualizacao.username() == null ? null : normalizarUsername(atualizacao.username());

        Set<String> conflitos = new LinkedHashSet<>();
        if (username != null && userRepository.existsByUsernameAndIdNot(username, id)) {
            conflitos.add(CampoJaUsadoException.USERNAME);
        }
        if (email != null && userRepository.existsByEmailAndIdNot(email, id)) {
            conflitos.add(CampoJaUsadoException.EMAIL);
        }
        if (!conflitos.isEmpty()) {
            throw CampoJaUsadoException.naAtualizacao(conflitos);
        }

        if (email != null) {
            user.alterarEmail(email);
        }
        if (username != null) {
            user.alterarUsername(username);
        }
        if (atualizacao.password() != null) {
            user.alterarSenha(passwordEncoder.encode(atualizacao.password()));
        }
        if (atualizacao.bio() != null) {
            user.alterarBio(textoOuNulo(atualizacao.bio()));
        }
        if (atualizacao.image() != null) {
            user.alterarImagem(textoOuNulo(atualizacao.image()));
        }
        user.marcarAtualizado(clock.instant());

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw CampoJaUsadoException.naAtualizacao(camposDaViolacao(e));
        }
        log.info("Usuário {} atualizado", id);
        return autenticado(user);
    }

    private User carregar(long id) {
        return userRepository.findById(id).orElseThrow(UsuarioDoTokenInexistenteException::new);
    }

    private UsuarioAutenticado autenticado(User user) {
        return new UsuarioAutenticado(user.getEmail(), tokenService.emitir(user.getId()), user.getUsername(),
                user.getBio(), user.getImage());
    }

    private static String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizarUsername(String username) {
        return username.strip();
    }

    private static String textoOuNulo(String valor) {
        return valor.isBlank() ? null : valor;
    }

    /** Traduz a violação de unicidade (corrida entre requisições) no campo correspondente. */
    private static Set<String> camposDaViolacao(DataIntegrityViolationException e) {
        String mensagem = String.valueOf(NestedExceptionUtils.getMostSpecificCause(e).getMessage());
        Set<String> campos = new LinkedHashSet<>();
        if (mensagem.contains(RESTRICAO_USERNAME)) {
            campos.add(CampoJaUsadoException.USERNAME);
        }
        if (mensagem.contains(RESTRICAO_EMAIL)) {
            campos.add(CampoJaUsadoException.EMAIL);
        }
        if (campos.isEmpty()) {
            throw e;
        }
        return campos;
    }
}
