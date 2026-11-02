package com.example.blog.perfil.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.perfil.persistence.Seguimento;
import com.example.blog.perfil.persistence.SeguimentoId;
import com.example.blog.perfil.persistence.SeguimentoRepository;
import com.example.blog.shared.web.RecursoNaoEncontradoException;
import com.example.blog.shared.web.ValidacaoException;
import com.example.blog.usuario.service.UsuarioPublico;
import com.example.blog.usuario.service.UsuarioService;

/**
 * Consulta de perfis e seguimento entre usuários. Seguir e deixar de seguir são idempotentes.
 */
@Service
public class PerfilService {

    static final String RECURSO_PERFIL = "profile";
    static final String MENSAGEM_SEGUIR_A_SI_MESMO = "cannot follow yourself";

    private final UsuarioService usuarioService;
    private final SeguimentoRepository seguimentoRepository;
    private final Clock clock;

    public PerfilService(UsuarioService usuarioService, SeguimentoRepository seguimentoRepository, Clock clock) {
        this.usuarioService = usuarioService;
        this.seguimentoRepository = seguimentoRepository;
        this.clock = clock;
    }

    /**
     * Consulta anônima: {@code following} é sempre falso.
     *
     * @throws RecursoNaoEncontradoException se o username não existir
     */
    @Transactional(readOnly = true)
    public Perfil consultar(String username) {
        return perfil(buscarUsuario(username), false);
    }

    /**
     * Consulta por um usuário autenticado: {@code following} indica se ele segue o perfil.
     *
     * @throws RecursoNaoEncontradoException se o username não existir
     */
    @Transactional(readOnly = true)
    public Perfil consultar(String username, Long seguidorId) {
        UsuarioPublico alvo = buscarUsuario(username);
        return perfil(alvo, seguimentoRepository.existsById(new SeguimentoId(seguidorId, alvo.id())));
    }

    /**
     * @throws RecursoNaoEncontradoException se o username não existir
     * @throws ValidacaoException            se o usuário tentar seguir a si mesmo
     */
    @Transactional
    public Perfil seguir(Long seguidorId, String username) {
        UsuarioPublico alvo = buscarUsuario(username);
        if (alvo.id().equals(seguidorId)) {
            throw new ValidacaoException(RECURSO_PERFIL, MENSAGEM_SEGUIR_A_SI_MESMO);
        }
        SeguimentoId id = new SeguimentoId(seguidorId, alvo.id());
        if (!seguimentoRepository.existsById(id)) {
            seguimentoRepository.save(new Seguimento(id, Instant.now(clock)));
        }
        return perfil(alvo, true);
    }

    /**
     * @throws RecursoNaoEncontradoException se o username não existir
     */
    @Transactional
    public Perfil deixarDeSeguir(Long seguidorId, String username) {
        UsuarioPublico alvo = buscarUsuario(username);
        SeguimentoId id = new SeguimentoId(seguidorId, alvo.id());
        if (seguimentoRepository.existsById(id)) {
            seguimentoRepository.deleteById(id);
        }
        return perfil(alvo, false);
    }

    private UsuarioPublico buscarUsuario(String username) {
        return usuarioService.buscarPublicoPorUsername(username)
                .orElseThrow(() -> new RecursoNaoEncontradoException(RECURSO_PERFIL));
    }

    private static Perfil perfil(UsuarioPublico usuario, boolean isFollowing) {
        return new Perfil(usuario.username(), usuario.bio(), usuario.image(), isFollowing);
    }
}
