package com.example.blog.profile.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.profile.persistence.FollowId;
import com.example.blog.profile.persistence.FollowRepository;
import com.example.blog.shared.error.ResourceNotFoundException;
import com.example.blog.shared.error.UnauthorizedException;
import com.example.blog.shared.error.UnprocessableEntityException;
import com.example.blog.shared.security.AuthenticatedUser;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

/** Consulta de perfis e relação de seguir/deixar de seguir. */
@Service
public class ProfileService {

    private static final String PROFILE = "profile";

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final Clock clock;

    public ProfileService(UserRepository userRepository, FollowRepository followRepository, Clock clock) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.clock = clock;
    }

    /**
     * Perfil consultado sem autenticação; {@code following} é sempre {@code false}.
     *
     * @throws ResourceNotFoundException se o usuário não existir
     */
    @Transactional(readOnly = true)
    public ProfileResult getProfile(String username) {
        return toResult(findByUsername(username), false);
    }

    /**
     * Perfil consultado por um usuário autenticado.
     *
     * @throws ResourceNotFoundException se o usuário não existir
     */
    @Transactional(readOnly = true)
    public ProfileResult getProfile(String username, AuthenticatedUser viewer) {
        User user = findByUsername(username);
        return toResult(user, followRepository.existsById(new FollowId(viewer.id(), user.getId())));
    }

    /**
     * Passa a seguir o usuário. Seguir quem já é seguido não tem efeito.
     *
     * @throws ResourceNotFoundException se o usuário não existir
     * @throws UnprocessableEntityException se o usuário tentar seguir a si mesmo
     */
    @Transactional
    public ProfileResult follow(AuthenticatedUser follower, String username) {
        User followed = findTarget(follower, username);
        followRepository.insertIfAbsent(follower.id(), followed.getId(), clock.instant());
        return toResult(followed, true);
    }

    /**
     * Deixa de seguir o usuário. Deixar de seguir quem não é seguido não tem efeito.
     *
     * @throws ResourceNotFoundException se o usuário não existir
     * @throws UnprocessableEntityException se o usuário tentar deixar de seguir a si mesmo
     */
    @Transactional
    public ProfileResult unfollow(AuthenticatedUser follower, String username) {
        User followed = findTarget(follower, username);
        followRepository.deleteRelation(follower.id(), followed.getId());
        return toResult(followed, false);
    }

    private User findTarget(AuthenticatedUser follower, String username) {
        User followed = findByUsername(username);
        if (followed.getId() == follower.id()) {
            throw new UnprocessableEntityException(PROFILE, "can't follow yourself");
        }
        if (!userRepository.existsById(follower.id())) {
            throw new UnauthorizedException("token", "is invalid");
        }
        return followed;
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException(PROFILE));
    }

    private static ProfileResult toResult(User user, boolean following) {
        return new ProfileResult(user.getUsername(), user.getBio(), user.getImage(), following);
    }
}
