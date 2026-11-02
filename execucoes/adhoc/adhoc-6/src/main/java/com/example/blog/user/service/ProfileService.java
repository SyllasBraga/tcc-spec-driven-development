package com.example.blog.user.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.shared.error.ValidationException;
import com.example.blog.user.persistence.UserAccount;
import com.example.blog.user.persistence.UserAccountRepository;
import com.example.blog.user.persistence.UserFollowId;
import com.example.blog.user.persistence.UserFollowRepository;

/**
 * Public profiles and the follow relationship between users.
 */
@Service
public class ProfileService {

    private final UserAccountRepository users;
    private final UserFollowRepository follows;
    private final Clock clock;

    public ProfileService(UserAccountRepository users, UserFollowRepository follows, Clock clock) {
        this.users = users;
        this.follows = follows;
        this.clock = clock;
    }

    /**
     * Profile as seen by an anonymous viewer.
     *
     * @throws ProfileNotFoundException if no user has this username
     */
    @Transactional(readOnly = true)
    public ProfileData getProfile(String username) {
        return ProfileData.of(findByUsername(username), false);
    }

    /**
     * Profile as seen by an authenticated viewer, telling whether the viewer follows it.
     *
     * @throws ProfileNotFoundException if no user has this username
     */
    @Transactional(readOnly = true)
    public ProfileData getProfile(String username, long viewerId) {
        UserAccount user = findByUsername(username);
        return ProfileData.of(user, follows.existsById(new UserFollowId(viewerId, user.getId())));
    }

    /**
     * Makes the current user follow {@code username}. Following an already followed user has no effect.
     *
     * @throws ProfileNotFoundException if no user has this username
     * @throws ValidationException if the user tries to follow themselves
     * @throws CurrentUserNotFoundException if the user of the token no longer exists
     */
    @Transactional
    public ProfileData follow(long followerId, String username) {
        UserAccount followed = findByUsername(username);
        if (followed.getId() == followerId) {
            throw new ValidationException("profile", "can't follow yourself");
        }
        ensureCurrentUserExists(followerId);
        follows.insertIfAbsent(followerId, followed.getId(), clock.instant());
        return ProfileData.of(followed, true);
    }

    /**
     * Makes the current user stop following {@code username}. Unfollowing a user not followed has no effect.
     *
     * @throws ProfileNotFoundException if no user has this username
     * @throws CurrentUserNotFoundException if the user of the token no longer exists
     */
    @Transactional
    public ProfileData unfollow(long followerId, String username) {
        UserAccount followed = findByUsername(username);
        ensureCurrentUserExists(followerId);
        follows.deleteFollow(followerId, followed.getId());
        return ProfileData.of(followed, false);
    }

    private UserAccount findByUsername(String username) {
        return users.findByUsername(username).orElseThrow(ProfileNotFoundException::new);
    }

    private void ensureCurrentUserExists(long userId) {
        if (!users.existsById(userId)) {
            throw new CurrentUserNotFoundException();
        }
    }
}
