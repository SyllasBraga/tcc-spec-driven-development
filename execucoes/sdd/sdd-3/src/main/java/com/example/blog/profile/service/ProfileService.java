package com.example.blog.profile.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.profile.persistence.FollowId;
import com.example.blog.profile.persistence.FollowRepository;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

/**
 * Perfil público e relação de seguir.
 */
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    public ProfileService(UserRepository userRepository, FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    /**
     * @param viewerId usuário autenticado, ou vazio em requisição anônima
     * @throws PerfilNaoEncontradoException se o nome de usuário não existir (diferencia maiúsculas)
     */
    @Transactional(readOnly = true)
    public Perfil buscar(String username, Optional<Long> viewerId) {
        User alvo = carregar(username);
        boolean following = viewerId
                .map(viewer -> followRepository.existsById(new FollowId(viewer, alvo.getId())))
                .orElse(false);
        return perfil(alvo, following);
    }

    /**
     * Passa a seguir o perfil; repetir não duplica a relação.
     *
     * @throws PerfilNaoEncontradoException se o nome de usuário não existir
     * @throws SeguirASiMesmoException se o alvo for o próprio usuário
     */
    @Transactional
    public Perfil seguir(long viewerId, String username) {
        User alvo = carregar(username);
        if (alvo.getId() == viewerId) {
            throw new SeguirASiMesmoException();
        }
        followRepository.seguir(viewerId, alvo.getId());
        return perfil(alvo, true);
    }

    /**
     * Deixa de seguir o perfil; sem efeito se não seguia.
     *
     * @throws PerfilNaoEncontradoException se o nome de usuário não existir
     */
    @Transactional
    public Perfil deixarDeSeguir(long viewerId, String username) {
        User alvo = carregar(username);
        followRepository.deixarDeSeguir(viewerId, alvo.getId());
        return perfil(alvo, false);
    }

    private User carregar(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new PerfilNaoEncontradoException(username));
    }

    private static Perfil perfil(User user, boolean following) {
        return new Perfil(user.getUsername(), user.getBio(), user.getImage(), following);
    }
}
