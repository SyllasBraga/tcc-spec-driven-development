package com.example.blog.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.profile.persistence.FollowId;
import com.example.blog.profile.persistence.FollowRepository;
import com.example.blog.shared.error.ErrorMessages;
import com.example.blog.shared.error.NotFoundException;
import com.example.blog.shared.error.ValidationException;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

/**
 * Consulta de perfis e relações de "seguir" entre usuários.
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
     * Perfil consultado sem autenticação: {@code following} é sempre {@code false}.
     *
     * @throws NotFoundException se o usuário não existir
     */
    @Transactional(readOnly = true)
    public ProfileData getProfile(String username) {
        return ProfileData.of(findByUsername(username), false);
    }

    /**
     * Perfil consultado pelo usuário {@code viewerId}.
     *
     * @throws NotFoundException se o usuário não existir
     */
    @Transactional(readOnly = true)
    public ProfileData getProfile(String username, long viewerId) {
        User user = findByUsername(username);
        boolean following = followRepository.existsById(new FollowId(viewerId, user.getId()));
        return ProfileData.of(user, following);
    }

    /**
     * Faz {@code followerId} seguir {@code username}. Seguir quem já é seguido não muda nada.
     *
     * @throws NotFoundException se o usuário não existir
     * @throws ValidationException se o usuário tentar seguir a si mesmo
     */
    @Transactional
    public ProfileData follow(long followerId, String username) {
        User user = findByUsername(username);
        if (user.getId() == followerId) {
            throw new ValidationException("profile", "cannot follow yourself");
        }
        followRepository.insertIfAbsent(followerId, user.getId());
        return ProfileData.of(user, true);
    }

    /**
     * Faz {@code followerId} deixar de seguir {@code username}. Deixar de seguir quem não é seguido não muda nada.
     *
     * @throws NotFoundException se o usuário não existir
     */
    @Transactional
    public ProfileData unfollow(long followerId, String username) {
        User user = findByUsername(username);
        followRepository.deleteIfPresent(followerId, user.getId());
        return ProfileData.of(user, false);
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("profile", ErrorMessages.NOT_FOUND));
    }
}
