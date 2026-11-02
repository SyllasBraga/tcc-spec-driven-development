package com.example.blog.profile.service;

import com.example.blog.profile.persistence.FollowId;
import com.example.blog.profile.persistence.FollowRepository;
import com.example.blog.shared.error.NotFoundException;
import com.example.blog.shared.error.UnprocessableException;
import com.example.blog.user.persistence.AppUser;
import com.example.blog.user.persistence.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta de perfis públicos e relações de "seguir". */
@Service
public class ProfileService {

    private static final String PROFILE = "profile";

    private final AppUserRepository userRepository;
    private final FollowRepository followRepository;

    public ProfileService(AppUserRepository userRepository, FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    /**
     * Busca o perfil pelo username.
     *
     * @param viewerId id de quem consulta, ou {@code null} se anônimo (então {@code following} é falso)
     * @throws NotFoundException se o username não existir
     */
    @Transactional(readOnly = true)
    public ProfileData getProfile(String username, Long viewerId) {
        AppUser target = findByUsername(username);
        boolean following = viewerId != null
                && followRepository.existsById(new FollowId(viewerId, target.getId()));
        return toProfile(target, following);
    }

    /**
     * Passa a seguir o usuário; repetir a operação não tem efeito adicional.
     *
     * @throws NotFoundException      se o username não existir
     * @throws UnprocessableException se o usuário tentar seguir a si mesmo
     */
    @Transactional
    public ProfileData follow(long followerId, String username) {
        AppUser target = findByUsername(username);
        if (target.getId() == followerId) {
            throw new UnprocessableException(PROFILE, "cannot follow yourself");
        }
        followRepository.insertIfAbsent(followerId, target.getId());
        return toProfile(target, true);
    }

    /**
     * Deixa de seguir o usuário; não seguir previamente não é erro.
     *
     * @throws NotFoundException se o username não existir
     */
    @Transactional
    public ProfileData unfollow(long followerId, String username) {
        AppUser target = findByUsername(username);
        followRepository.delete(followerId, target.getId());
        return toProfile(target, false);
    }

    private AppUser findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException(PROFILE));
    }

    private static ProfileData toProfile(AppUser user, boolean following) {
        return new ProfileData(user.getUsername(), user.getBio(), user.getImage(), following);
    }
}
