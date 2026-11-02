package com.example.blog.profile.service;

import com.example.blog.profile.persistence.FollowId;
import com.example.blog.profile.persistence.FollowRepository;
import com.example.blog.shared.error.InvalidTokenException;
import com.example.blog.shared.error.NotFoundException;
import com.example.blog.shared.error.ValidationException;
import com.example.blog.user.persistence.UserEntity;
import com.example.blog.user.persistence.UserRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de perfis públicos e relação de seguir. Seguir e deixar de seguir são idempotentes.
 */
@Service
public class ProfileService {

    private static final String RECURSO = "profile";

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final Clock clock;

    public ProfileService(UserRepository userRepository, FollowRepository followRepository, Clock clock) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.clock = clock;
    }

    /**
     * @param username       nome do perfil (diferencia maiúsculas)
     * @param viewerIdOuNulo id de quem consulta, ou {@code null} se anônimo
     * @throws NotFoundException se o nome não existir
     */
    @Transactional(readOnly = true)
    public Profile buscarPerfil(String username, Long viewerIdOuNulo) {
        UserEntity alvo = buscarAlvo(username);
        boolean following = viewerIdOuNulo != null
                && followRepository.existsById(new FollowId(viewerIdOuNulo, alvo.getId()));
        return paraPerfil(alvo, following);
    }

    /**
     * Faz o usuário autenticado seguir o perfil. Repetir a operação não cria uma segunda relação.
     *
     * @throws InvalidTokenException se o seguidor não existir mais
     * @throws NotFoundException     se o nome não existir
     * @throws ValidationException   se o perfil for o do próprio seguidor
     */
    @Transactional
    public Profile seguir(String username, Long followerId) {
        exigirSeguidor(followerId);
        UserEntity alvo = buscarAlvo(username);
        if (alvo.getId().equals(followerId)) {
            throw new ValidationException(RECURSO, "cannot follow yourself");
        }
        FollowId id = new FollowId(followerId, alvo.getId());
        if (!followRepository.existsById(id)) {
            followRepository.inserirSeAusente(followerId, alvo.getId(), clock.instant());
        }
        return paraPerfil(alvo, true);
    }

    /**
     * Remove a relação, se existir.
     *
     * @throws InvalidTokenException se o seguidor não existir mais
     * @throws NotFoundException     se o nome não existir
     */
    @Transactional
    public Profile deixarDeSeguir(String username, Long followerId) {
        exigirSeguidor(followerId);
        UserEntity alvo = buscarAlvo(username);
        followRepository.deleteById(new FollowId(followerId, alvo.getId()));
        return paraPerfil(alvo, false);
    }

    private void exigirSeguidor(Long followerId) {
        if (!userRepository.existsById(followerId)) {
            throw new InvalidTokenException("Token de usuário inexistente: " + followerId);
        }
    }

    private UserEntity buscarAlvo(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException(RECURSO));
    }

    private static Profile paraPerfil(UserEntity usuario, boolean following) {
        return new Profile(usuario.getUsername(), usuario.getBio(), usuario.getImage(), following);
    }
}
