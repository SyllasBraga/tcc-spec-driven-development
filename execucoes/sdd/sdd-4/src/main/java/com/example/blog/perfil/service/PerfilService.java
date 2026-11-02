package com.example.blog.perfil.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.perfil.persistence.SeguimentoId;
import com.example.blog.perfil.persistence.SeguimentoRepository;
import com.example.blog.shared.erro.RecursoNaoEncontradoException;
import com.example.blog.shared.erro.TokenInvalidoException;
import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.usuario.service.DadosPublicosUsuario;
import com.example.blog.usuario.service.UsuarioService;

/** Consulta de perfis e relações de seguir entre usuários. */
@Service
public class PerfilService {

    private static final String CHAVE_PERFIL = "profile";
    private static final String NAO_PODE_SEGUIR_A_SI_MESMO = "cannot follow yourself";

    private final UsuarioService usuarioService;
    private final SeguimentoRepository seguimentoRepository;
    private final Clock clock;

    public PerfilService(UsuarioService usuarioService, SeguimentoRepository seguimentoRepository, Clock clock) {
        this.usuarioService = usuarioService;
        this.seguimentoRepository = seguimentoRepository;
        this.clock = clock;
    }

    /**
     * Perfil consultado por quem não está autenticado; {@code following} é sempre {@code false}.
     *
     * @throws RecursoNaoEncontradoException se o usuário não existir
     */
    @Transactional(readOnly = true)
    public Perfil buscar(String username) {
        return perfil(usuarioService.buscarPorUsername(username), false);
    }

    /**
     * Perfil consultado pelo usuário autenticado, indicando se ele segue o perfil.
     *
     * @throws RecursoNaoEncontradoException se o usuário consultado não existir
     * @throws TokenInvalidoException se o usuário autenticado não existir mais
     */
    @Transactional(readOnly = true)
    public Perfil buscarComo(long idUsuarioAtual, String username) {
        usuarioService.confirmarExistencia(idUsuarioAtual);
        DadosPublicosUsuario alvo = usuarioService.buscarPorUsername(username);
        boolean segue = seguimentoRepository.existsById(new SeguimentoId(idUsuarioAtual, alvo.id()));
        return perfil(alvo, segue);
    }

    /**
     * Faz o usuário autenticado seguir o alvo. Seguir quem já é seguido não altera nada.
     *
     * @throws ValidacaoException se o alvo for o próprio usuário
     * @throws RecursoNaoEncontradoException se o alvo não existir
     * @throws TokenInvalidoException se o usuário autenticado não existir mais
     */
    @Transactional
    public Perfil seguir(long idUsuarioAtual, String username) {
        usuarioService.confirmarExistencia(idUsuarioAtual);
        DadosPublicosUsuario alvo = usuarioService.buscarPorUsername(username);
        if (alvo.id() == idUsuarioAtual) {
            throw new ValidacaoException(CHAVE_PERFIL, NAO_PODE_SEGUIR_A_SI_MESMO);
        }
        seguimentoRepository.seguir(idUsuarioAtual, alvo.id(), Instant.now(clock));
        return perfil(alvo, true);
    }

    /**
     * Faz o usuário autenticado deixar de seguir o alvo. Deixar de seguir quem não é seguido não altera nada.
     *
     * @throws RecursoNaoEncontradoException se o alvo não existir
     * @throws TokenInvalidoException se o usuário autenticado não existir mais
     */
    @Transactional
    public Perfil deixarDeSeguir(long idUsuarioAtual, String username) {
        usuarioService.confirmarExistencia(idUsuarioAtual);
        DadosPublicosUsuario alvo = usuarioService.buscarPorUsername(username);
        seguimentoRepository.deixarDeSeguir(idUsuarioAtual, alvo.id());
        return perfil(alvo, false);
    }

    private static Perfil perfil(DadosPublicosUsuario usuario, boolean following) {
        return new Perfil(usuario.username(), usuario.bio(), usuario.image(), following);
    }
}
