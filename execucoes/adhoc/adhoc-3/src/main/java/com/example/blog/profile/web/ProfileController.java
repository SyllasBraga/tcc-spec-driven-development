package com.example.blog.profile.web;

import com.example.blog.profile.service.ProfileService;
import com.example.blog.shared.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Perfis públicos e seguir/deixar de seguir. */
@RestController
@RequestMapping("/api/profiles/{username}")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /** Autenticação opcional: {@code currentUser} é {@code null} em requisições anônimas. */
    @GetMapping
    public ProfileResponse getProfile(@PathVariable String username, AuthenticatedUser currentUser) {
        Long viewerId = currentUser == null ? null : currentUser.id();
        return ProfileResponse.from(profileService.getProfile(username, viewerId));
    }

    @PostMapping("/follow")
    public ProfileResponse follow(@PathVariable String username, AuthenticatedUser currentUser) {
        return ProfileResponse.from(profileService.follow(currentUser.id(), username));
    }

    @DeleteMapping("/follow")
    public ProfileResponse unfollow(@PathVariable String username, AuthenticatedUser currentUser) {
        return ProfileResponse.from(profileService.unfollow(currentUser.id(), username));
    }
}
