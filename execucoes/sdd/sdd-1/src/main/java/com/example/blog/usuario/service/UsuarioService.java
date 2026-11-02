package com.example.blog.usuario.service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.shared.security.TokenInvalidoException;
import com.example.blog.shared.security.TokenService;
import com.example.blog.shared.web.ConflitoException;
import com.example.blog.shared.web.CredenciaisInvalidasException;
import com.example.blog.shared.web.ValidacaoException;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;

/**
 * Regras de cadastro, login e manutenção do usuário autenticado.
 */
@Service
public class UsuarioService {

    static final String MENSAGEM_JA_USADO = "has already been taken";

    private static final String CAMPO_EMAIL = "email";
    private static final String CAMPO_USERNAME = "username";
    private static final String RESTRICAO_EMAIL = "uq_usuario_email";
    private static final String RESTRICAO_USERNAME = "uq_usuario_username";

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
     * Cadastra um usuário e emite um token para ele.
     *
     * @throws ConflitoException se e-mail ou username já pertencerem a outro usuário
     */
    @Transactional
    public UsuarioAutenticado registrar(String username, String email, String senha) {
        String emailNormalizado = normalizarEmail(email);
        String usernameNormalizado = username.trim();
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ConflitoException(CAMPO_EMAIL, MENSAGEM_JA_USADO);
        }
        if (usuarioRepository.existsByUsername(usernameNormalizado)) {
            throw new ConflitoException(CAMPO_USERNAME, MENSAGEM_JA_USADO);
        }
        Usuario usuario = new Usuario(emailNormalizado, usernameNormalizado, passwordEncoder.encode(senha),
                Instant.now(clock));
        Usuario salvo = gravar(usuario, ConflitoException::new);
        return autenticado(salvo, tokenService.emitir(salvo.getId()));
    }

    /**
     * Confere as credenciais e emite um novo token.
     *
     * @throws CredenciaisInvalidasException se o e-mail não existir ou a senha não conferir
     */
    @Transactional(readOnly = true)
    public UsuarioAutenticado entrar(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(email))
                .filter(encontrado -> passwordEncoder.matches(senha, encontrado.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);
        return autenticado(usuario, tokenService.emitir(usuario.getId()));
    }

    /**
     * Busca o usuário dono do token.
     *
     * @param token token recebido, devolvido sem alteração
     * @throws TokenInvalidoException se o usuário não existir mais
     */
    @Transactional(readOnly = true)
    public UsuarioAutenticado buscarAtual(Long usuarioId, String token) {
        return autenticado(buscarPorId(usuarioId), token);
    }

    /**
     * Atualiza somente os campos informados no comando.
     *
     * @throws ValidacaoException     se e-mail ou username já pertencerem a outro usuário
     * @throws TokenInvalidoException se o usuário não existir mais
     */
    @Transactional
    public UsuarioAutenticado atualizar(Long usuarioId, String token, AtualizacaoUsuario comando) {
        Usuario usuario = buscarPorId(usuarioId);
        String novoEmail = comando.email().isInformado() ? normalizarEmail(comando.email().valor()) : null;
        String novoUsername = comando.username().isInformado() ? comando.username().valor().trim() : null;
        // Checa duplicidade antes de alterar a entidade, para o auto-flush das consultas não gravar nada.
        Map<String, List<String>> duplicados = new LinkedHashMap<>();
        if (novoEmail != null && usuarioRepository.existsByEmailAndIdNot(novoEmail, usuarioId)) {
            duplicados.put(CAMPO_EMAIL, List.of(MENSAGEM_JA_USADO));
        }
        if (novoUsername != null && usuarioRepository.existsByUsernameAndIdNot(novoUsername, usuarioId)) {
            duplicados.put(CAMPO_USERNAME, List.of(MENSAGEM_JA_USADO));
        }
        if (!duplicados.isEmpty()) {
            throw new ValidacaoException(duplicados);
        }
        Instant agora = Instant.now(clock);
        if (novoEmail != null) {
            usuario.alterarEmail(novoEmail, agora);
        }
        if (novoUsername != null) {
            usuario.alterarUsername(novoUsername, agora);
        }
        if (comando.password().isInformado()) {
            usuario.alterarSenhaHash(passwordEncoder.encode(comando.password().valor()), agora);
        }
        if (comando.bio().isInformado()) {
            usuario.alterarBio(comando.bio().valor(), agora);
        }
        if (comando.image().isInformado()) {
            usuario.alterarImage(comando.image().valor(), agora);
        }
        return autenticado(gravar(usuario, ValidacaoException::new), token);
    }

    /**
     * Busca os dados públicos de um usuário pelo username exato.
     */
    @Transactional(readOnly = true)
    public Optional<UsuarioPublico> buscarPublicoPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .map(usuario -> new UsuarioPublico(
                        usuario.getId(), usuario.getUsername(), usuario.getBio(), usuario.getImage()));
    }

    private Usuario buscarPorId(Long usuarioId) {
        return usuarioRepository.findById(usuarioId).orElseThrow(TokenInvalidoException::new);
    }

    /**
     * Grava e força o flush para que a violação de unicidade do banco (concorrência) vire o mesmo
     * erro de duplicidade da checagem prévia.
     */
    private Usuario gravar(Usuario usuario, BiFunction<String, String, ? extends RuntimeException> erroDeDuplicidade) {
        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            String campo = campoDuplicado(ex).orElseThrow(() -> ex);
            throw erroDeDuplicidade.apply(campo, MENSAGEM_JA_USADO);
        }
    }

    private static Optional<String> campoDuplicado(DataIntegrityViolationException ex) {
        String mensagem = String.valueOf(ex.getMostSpecificCause().getMessage());
        if (mensagem.contains(RESTRICAO_EMAIL)) {
            return Optional.of(CAMPO_EMAIL);
        }
        if (mensagem.contains(RESTRICAO_USERNAME)) {
            return Optional.of(CAMPO_USERNAME);
        }
        return Optional.empty();
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static UsuarioAutenticado autenticado(Usuario usuario, String token) {
        return new UsuarioAutenticado(
                usuario.getEmail(), token, usuario.getUsername(), usuario.getBio(), usuario.getImage());
    }
}
