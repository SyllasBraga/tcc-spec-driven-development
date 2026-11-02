package com.example.blog.profile.service;

import java.time.Clock;

import com.example.blog.profile.persistence.FollowId;
import com.example.blog.profile.persistence.FollowRepository;
import com.example.blog.shared.error.NotFoundException;
import com.example.blog.shared.error.UnprocessableException;
import com.example.blog.shared.error.ValidationMessages;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta de perfis e relações de "seguir" entre usuários. */
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final Clock clock;

    public ProfileService(UserRepository userRepository, FollowRepository followRepository, Clock clock) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.clock = clock;
    }

    /**
     * Busca o perfil para um visitante anônimo ({@code following} é sempre falso).
     *
     * @throws NotFoundException se o usuário não existir
     */
    @Transactional(readOnly = true)
    public ProfileView getProfile(String username) {
        return ProfileView.of(loadByUsername(username), false);
    }

    /**
     * Busca o perfil do ponto de vista do usuário autenticado.
     *
     * @throws NotFoundException se o usuário não existir
     */
    @Transactional(readOnly = true)
    public ProfileView getProfile(String username, long viewerId) {
        User target = loadByUsername(username);
        return ProfileView.of(target, isFollowing(viewerId, target));
    }

    /**
     * Passa a seguir o usuário. Seguir quem já é seguido não tem efeito.
     *
     * @throws NotFoundException se o usuário não existir
     * @throws UnprocessableException se o usuário tentar seguir a si mesmo
     */
    @Transactional
    public ProfileView follow(long followerId, String username) {
        User target = loadByUsername(username);
        if (target.getId() == followerId) {
            throw new UnprocessableException("username", "cannot follow yourself");
        }
        followRepository.insertIfAbsent(followerId, target.getId(), clock.instant());
        return ProfileView.of(target, true);
    }

    /**
     * Deixa de seguir o usuário. Deixar de seguir quem não é seguido não tem efeito.
     *
     * @throws NotFoundException se o usuário não existir
     */
    @Transactional
    public ProfileView unfollow(long followerId, String username) {
        User target = loadByUsername(username);
        followRepository.deleteRelation(followerId, target.getId());
        return ProfileView.of(target, false);
    }

    private boolean isFollowing(long viewerId, User target) {
        return followRepository.existsById(new FollowId(viewerId, target.getId()));
    }

    private User loadByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("profile", ValidationMessages.NOT_FOUND));
    }
}
