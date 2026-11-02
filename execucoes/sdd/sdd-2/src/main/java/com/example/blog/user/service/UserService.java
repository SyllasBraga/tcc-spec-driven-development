package com.example.blog.user.service;

import com.example.blog.shared.error.ConflictException;
import com.example.blog.shared.error.InvalidCredentialsException;
import com.example.blog.shared.error.InvalidTokenException;
import com.example.blog.shared.error.ValidationException;
import com.example.blog.shared.security.TokenService;
import com.example.blog.user.persistence.UserEntity;
import com.example.blog.user.persistence.UserRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de cadastro, autenticação e manutenção da conta do usuário.
 */
@Service
public class UserService {

    private static final String CANT_BE_BLANK = "can't be blank";
    private static final String CAMPO_EMAIL = "email";
    private static final String CAMPO_USERNAME = "username";
    private static final String CAMPO_PASSWORD = "password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailFormat emailFormat;
    private final Clock clock;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
            EmailFormat emailFormat, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailFormat = emailFormat;
        this.clock = clock;
    }

    /**
     * Cadastra um usuário. Espaços nas pontas de {@code username} e {@code email} são removidos.
     *
     * @return o usuário criado, com token
     * @throws ConflictException com as chaves {@code email} e/ou {@code username} já usadas por outro usuário
     */
    @Transactional
    public AuthenticatedUser cadastrar(String username, String email, String password) {
        String usernameNormalizado = username.trim();
        String emailNormalizado = email.trim();

        List<String> conflitos = new ArrayList<>();
        if (userRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            conflitos.add(CAMPO_EMAIL);
        }
        if (userRepository.existsByUsername(usernameNormalizado)) {
            conflitos.add(CAMPO_USERNAME);
        }
        if (!conflitos.isEmpty()) {
            throw new ConflictException(conflitos);
        }

        UserEntity usuario = userRepository.save(new UserEntity(
                usernameNormalizado, emailNormalizado, passwordEncoder.encode(password), clock.instant()));
        return comNovoToken(usuario);
    }

    /**
     * Autentica por e-mail (sem diferenciar maiúsculas) e senha.
     *
     * @return o usuário, com um novo token
     * @throws InvalidCredentialsException se o e-mail não existir ou a senha não conferir
     */
    @Transactional(readOnly = true)
    public AuthenticatedUser autenticar(String email, String password) {
        return userRepository.findByEmailIgnoreCase(email.trim())
                .filter(usuario -> passwordEncoder.matches(password, usuario.getPasswordHash()))
                .map(this::comNovoToken)
                .orElseThrow(InvalidCredentialsException::new);
    }

    /**
     * @return o usuário do token, com um novo token
     * @throws InvalidTokenException se o usuário não existir mais
     */
    @Transactional(readOnly = true)
    public AuthenticatedUser buscarAtual(Long userId) {
        return comNovoToken(carregar(userId));
    }

    /**
     * Altera somente os campos enviados. {@code bio} e {@code image} enviados como nulos ou em branco são limpos
     * (gravados como {@code null}).
     *
     * @return o usuário atualizado, com um novo token
     * @throws ValidationException   se nenhum campo for enviado, ou se {@code email}, {@code username} ou
     *                               {@code password} vierem nulos, em branco ou (e-mail) com formato inválido
     * @throws InvalidTokenException se o usuário não existir mais
     * @throws ConflictException     se o e-mail ou o nome já pertencerem a outro usuário
     */
    @Transactional
    public AuthenticatedUser atualizar(Long userId, UserUpdate update) {
        if (update.isVazio()) {
            throw new ValidationException("user", "can't be empty");
        }
        String email = update.hasEmail() ? aparar(update.email()) : null;
        String username = update.hasUsername() ? aparar(update.username()) : null;
        validarCamposObrigatorios(update, email, username);

        UserEntity usuario = carregar(userId);
        verificarConflitos(usuario.getId(), email, username);

        if (email != null) {
            usuario.alterarEmail(email);
        }
        if (username != null) {
            usuario.alterarUsername(username);
        }
        if (update.hasPassword()) {
            usuario.alterarSenha(passwordEncoder.encode(update.password()));
        }
        if (update.hasBio()) {
            usuario.alterarBio(brancoParaNulo(update.bio()));
        }
        if (update.hasImage()) {
            usuario.alterarImage(brancoParaNulo(update.image()));
        }
        usuario.registrarAtualizacao(clock.instant());
        return comNovoToken(usuario);
    }

    private static String brancoParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    private void validarCamposObrigatorios(UserUpdate update, String email, String username) {
        Map<String, List<String>> erros = new LinkedHashMap<>();
        if (update.hasEmail()) {
            if (email == null || email.isEmpty()) {
                erros.put(CAMPO_EMAIL, List.of(CANT_BE_BLANK));
            } else if (!emailFormat.isValido(email)) {
                erros.put(CAMPO_EMAIL, List.of("is invalid"));
            }
        }
        if (update.hasUsername() && (username == null || username.isEmpty())) {
            erros.put(CAMPO_USERNAME, List.of(CANT_BE_BLANK));
        }
        if (update.hasPassword() && (update.password() == null || update.password().isBlank())) {
            erros.put(CAMPO_PASSWORD, List.of(CANT_BE_BLANK));
        }
        if (!erros.isEmpty()) {
            throw new ValidationException(erros);
        }
    }

    private void verificarConflitos(Long userId, String email, String username) {
        List<String> conflitos = new ArrayList<>();
        if (email != null && userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            conflitos.add(CAMPO_EMAIL);
        }
        if (username != null && userRepository.existsByUsernameAndIdNot(username, userId)) {
            conflitos.add(CAMPO_USERNAME);
        }
        if (!conflitos.isEmpty()) {
            throw new ConflictException(conflitos);
        }
    }

    private UserEntity carregar(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("Token de usuário inexistente: " + userId));
    }

    private AuthenticatedUser comNovoToken(UserEntity usuario) {
        return new AuthenticatedUser(usuario.getId(), usuario.getEmail(), usuario.getUsername(),
                usuario.getBio(), usuario.getImage(), tokenService.emitir(usuario.getId()));
    }

    private static String aparar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
