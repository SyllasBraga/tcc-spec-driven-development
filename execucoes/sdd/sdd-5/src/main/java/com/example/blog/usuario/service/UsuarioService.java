package com.example.blog.usuario.service;

import com.example.blog.shared.erro.ConflitoException;
import com.example.blog.shared.erro.CredenciaisInvalidasException;
import com.example.blog.shared.erro.MensagensErro;
import com.example.blog.shared.erro.TokenInvalidoException;
import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de conta: cadastro, login, consulta e atualização do usuário autenticado.
 *
 * <p>Toda resposta de sucesso traz um token novo, emitido por {@link TokenService}.
 */
@Service
public class UsuarioService {

    private static final String CAMPO_EMAIL = "email";
    private static final String CAMPO_USERNAME = "username";
    private static final Map<String, String> CAMPO_POR_RESTRICAO = Map.of(
            "uq_usuario_email", CAMPO_EMAIL,
            "uq_usuario_username", CAMPO_USERNAME);

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
     * Cria uma conta.
     *
     * @throws ConflitoException se o e-mail (sem diferenciar maiúsculas) ou o username já estiverem em uso
     */
    @Transactional
    public UsuarioAutenticadoDto cadastrar(String username, String email, String senha) {
        String usernameNormalizado = username.trim();
        String emailNormalizado = email.trim();
        Map<String, List<String>> conflitos = new TreeMap<>();
        if (repository.existsByEmailIgnoreCase(emailNormalizado)) {
            conflitos.put(CAMPO_EMAIL, List.of(MensagensErro.JA_EM_USO));
        }
        if (repository.existsByUsername(usernameNormalizado)) {
            conflitos.put(CAMPO_USERNAME, List.of(MensagensErro.JA_EM_USO));
        }
        if (!conflitos.isEmpty()) {
            throw new ConflitoException(conflitos);
        }
        Usuario usuario = new Usuario(usernameNormalizado, emailNormalizado, passwordEncoder.encode(senha),
                Instant.now(clock));
        try {
            return paraDto(repository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException excecao) {
            throw new ConflitoException(Map.of(campoDaViolacao(excecao), List.of(MensagensErro.JA_EM_USO)));
        }
    }

    /**
     * Autentica por e-mail (sem diferenciar maiúsculas) e senha.
     *
     * @throws CredenciaisInvalidasException se o e-mail não existir ou a senha não conferir
     */
    @Transactional(readOnly = true)
    public UsuarioAutenticadoDto autenticar(String email, String senha) {
        Optional<Usuario> usuario = repository.findByEmailIgnoreCase(email.trim());
        if (usuario.isEmpty() || !passwordEncoder.matches(senha, usuario.get().getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }
        return paraDto(usuario.get());
    }

    /**
     * @throws TokenInvalidoException se o usuário do token não existir
     */
    @Transactional(readOnly = true)
    public UsuarioAutenticadoDto buscarAtual(Long id) {
        return paraDto(buscarPorIdDoToken(id));
    }

    /**
     * Aplica somente os campos presentes na atualização.
     *
     * @throws TokenInvalidoException se o usuário do token não existir
     * @throws ValidacaoException se o e-mail ou o username pertencerem a outra conta
     */
    @Transactional
    public UsuarioAutenticadoDto atualizar(Long id, AtualizacaoUsuario atualizacao) {
        Usuario usuario = buscarPorIdDoToken(id);
        verificarUnicidade(id, atualizacao);
        if (atualizacao.email().presente()) {
            usuario.alterarEmail(atualizacao.email().valor());
        }
        if (atualizacao.username().presente()) {
            usuario.alterarUsername(atualizacao.username().valor());
        }
        if (atualizacao.senha().presente()) {
            usuario.alterarSenhaHash(passwordEncoder.encode(atualizacao.senha().valor()));
        }
        if (atualizacao.bio().presente()) {
            usuario.alterarBio(atualizacao.bio().valor());
        }
        if (atualizacao.imagem().presente()) {
            usuario.alterarImagem(atualizacao.imagem().valor());
        }
        usuario.registrarAlteracao(Instant.now(clock));
        try {
            return paraDto(repository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException excecao) {
            throw new ValidacaoException(Map.of(campoDaViolacao(excecao), List.of(MensagensErro.JA_EM_USO)));
        }
    }

    private void verificarUnicidade(Long id, AtualizacaoUsuario atualizacao) {
        Map<String, List<String>> conflitos = new TreeMap<>();
        if (atualizacao.email().presente()
                && repository.existsByEmailIgnoreCaseAndIdNot(atualizacao.email().valor(), id)) {
            conflitos.put(CAMPO_EMAIL, List.of(MensagensErro.JA_EM_USO));
        }
        if (atualizacao.username().presente()
                && repository.existsByUsernameAndIdNot(atualizacao.username().valor(), id)) {
            conflitos.put(CAMPO_USERNAME, List.of(MensagensErro.JA_EM_USO));
        }
        if (!conflitos.isEmpty()) {
            throw new ValidacaoException(conflitos);
        }
    }

    private Usuario buscarPorIdDoToken(Long id) {
        return repository.findById(id).orElseThrow(TokenInvalidoException::new);
    }

    private UsuarioAutenticadoDto paraDto(Usuario usuario) {
        return new UsuarioAutenticadoDto(usuario.getId(), usuario.getEmail(), usuario.getUsername(),
                usuario.getBio(), usuario.getImagem(), tokenService.emitir(usuario.getId()));
    }

    /** Identifica o campo pela restrição violada; violações não relacionadas à unicidade são relançadas. */
    private static String campoDaViolacao(DataIntegrityViolationException excecao) {
        Throwable causa = excecao;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violacao && violacao.getConstraintName() != null) {
                String campo = CAMPO_POR_RESTRICAO.get(violacao.getConstraintName().toLowerCase(Locale.ROOT));
                if (campo != null) {
                    return campo;
                }
            }
            causa = causa.getCause();
        }
        throw excecao;
    }
}
