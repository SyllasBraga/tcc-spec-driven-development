package com.example.blog.profile.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.profile.service.ProfileResult;
import com.example.blog.profile.service.ProfileService;
import com.example.blog.shared.security.AuthenticatedUser;

/** Perfis públicos e relação de seguir. */
@RestController
@RequestMapping("/api/profiles/{username}")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /** Autenticação opcional: sem token, {@code following} é {@code false}. */
    @GetMapping
    public ProfileResponse getProfile(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        ProfileResult result = jwt == null
                ? profileService.getProfile(username)
                : profileService.getProfile(username, AuthenticatedUser.from(jwt));
        return ProfileResponse.from(result);
    }

    @PostMapping("/follow")
    public ProfileResponse follow(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.from(profileService.follow(AuthenticatedUser.from(jwt), username));
    }

    @DeleteMapping("/follow")
    public ProfileResponse unfollow(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.from(profileService.unfollow(AuthenticatedUser.from(jwt), username));
    }
}
