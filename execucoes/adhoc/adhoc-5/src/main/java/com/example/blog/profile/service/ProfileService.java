package com.example.blog.profile.service;

import java.time.Clock;

import com.example.blog.profile.persistence.FollowId;
import com.example.blog.profile.persistence.FollowRepository;
import com.example.blog.shared.error.NotFoundException;
import com.example.blog.shared.error.UnprocessableException;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de perfis públicos e relação de seguir entre usuários.
 */
@Service
public class ProfileService {

    private static final String RECURSO_PERFIL = "profile";

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final Clock clock;

    public ProfileService(UserRepository userRepository, FollowRepository followRepository, Clock clock) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.clock = clock;
    }

    /**
     * Perfil visto por um visitante anônimo ({@code following} sempre {@code false}).
     *
     * @throws NotFoundException se o usuário não existir
     */
    @Transactional(readOnly = true)
    public ProfileData getProfile(String username) {
        return toProfile(findByUsername(username), false);
    }

    /**
     * Perfil visto pelo usuário autenticado {@code viewerId}.
     *
     * @throws NotFoundException se o usuário não existir
     */
    @Transactional(readOnly = true)
    public ProfileData getProfile(String username, long viewerId) {
        User target = findByUsername(username);
        return toProfile(target, followRepository.existsById(new FollowId(viewerId, target.getId())));
    }

    /**
     * Faz {@code followerId} seguir {@code username}. Seguir quem já é seguido não tem efeito.
     *
     * @throws NotFoundException      se o usuário não existir
     * @throws UnprocessableException se o usuário tentar seguir a si mesmo
     */
    @Transactional
    public ProfileData follow(long followerId, String username) {
        User target = findByUsername(username);
        if (target.getId() == followerId) {
            throw new UnprocessableException(RECURSO_PERFIL, "can't follow yourself");
        }
        followRepository.insertIgnoringDuplicate(followerId, target.getId(), clock.instant());
        return toProfile(target, true);
    }

    /**
     * Faz {@code followerId} deixar de seguir {@code username}. Deixar de seguir quem não é seguido não tem efeito.
     *
     * @throws NotFoundException se o usuário não existir
     */
    @Transactional
    public ProfileData unfollow(long followerId, String username) {
        User target = findByUsername(username);
        followRepository.deleteFollow(new FollowId(followerId, target.getId()));
        return toProfile(target, false);
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException(RECURSO_PERFIL));
    }

    private static ProfileData toProfile(User user, boolean following) {
        return new ProfileData(user.getUsername(), user.getBio(), user.getImage(), following);
    }
}
