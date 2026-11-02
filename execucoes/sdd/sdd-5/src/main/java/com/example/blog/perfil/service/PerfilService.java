package com.example.blog.perfil.service;

import com.example.blog.perfil.persistence.SeguimentoId;
import com.example.blog.perfil.persistence.SeguimentoRepository;
import com.example.blog.shared.erro.MensagensErro;
import com.example.blog.shared.erro.RecursoNaoEncontradoException;
import com.example.blog.shared.erro.TokenInvalidoException;
import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Perfis públicos e a relação de seguir entre usuários.
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
     * Consulta um perfil pelo username exato (sensível a maiúsculas).
     *
     * @param idAutenticadoOuNulo usuário do token, ou {@code null} em requisição anônima
     * @throws RecursoNaoEncontradoException se o username não existir
     * @throws TokenInvalidoException se o usuário do token não existir
     */
    @Transactional(readOnly = true)
    public PerfilDto buscarPerfil(String username, Long idAutenticadoOuNulo) {
        Usuario alvo = buscarAlvo(username);
        if (idAutenticadoOuNulo == null) {
            return paraDto(alvo, false);
        }
        confirmarAutenticado(idAutenticadoOuNulo);
        boolean seguindo = seguimentoRepository.existsById(new SeguimentoId(idAutenticadoOuNulo, alvo.getId()));
        return paraDto(alvo, seguindo);
    }

    /**
     * Passa a seguir o usuário; repetir a operação não duplica a relação.
     *
     * @throws ValidacaoException se o usuário tentar seguir a si mesmo
     */
    @Transactional
    public PerfilDto seguir(String username, Long idAutenticado) {
        Usuario alvo = buscarAlvo(username);
        confirmarAutenticado(idAutenticado);
        if (alvo.getId().equals(idAutenticado)) {
            throw ValidacaoException.de(MensagensErro.CHAVE_PROFILE, MensagensErro.NAO_PODE_SEGUIR_A_SI_MESMO);
        }
        seguimentoRepository.inserirSeNaoExistir(idAutenticado, alvo.getId(), Instant.now(clock));
        return paraDto(alvo, true);
    }

    /**
     * Deixa de seguir o usuário; não seguir antes não é erro.
     */
    @Transactional
    public PerfilDto deixarDeSeguir(String username, Long idAutenticado) {
        Usuario alvo = buscarAlvo(username);
        confirmarAutenticado(idAutenticado);
        SeguimentoId id = new SeguimentoId(idAutenticado, alvo.getId());
        if (seguimentoRepository.existsById(id)) {
            seguimentoRepository.deleteById(id);
        }
        return paraDto(alvo, false);
    }

    private Usuario buscarAlvo(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RecursoNaoEncontradoException(MensagensErro.CHAVE_PROFILE));
    }

    private void confirmarAutenticado(Long idAutenticado) {
        if (!usuarioRepository.existsById(idAutenticado)) {
            throw new TokenInvalidoException();
        }
    }

    private static PerfilDto paraDto(Usuario usuario, boolean seguindo) {
        return new PerfilDto(usuario.getUsername(), usuario.getBio(), usuario.getImagem(), seguindo);
    }
}
