package com.example.blog.profile.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.profile.service.ProfileData;
import com.example.blog.profile.service.ProfileService;
import com.example.blog.shared.security.AuthenticatedUser;
import com.example.blog.shared.web.ApiMediaTypes;

/**
 * Consulta de perfis (autenticação opcional) e seguir e deixar de seguir usuários (autenticação obrigatória).
 */
@RestController
@RequestMapping(path = "/api/profiles/{username}", produces = ApiMediaTypes.JSON)
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * @param jwt {@code null} quando a requisição não traz token
     */
    @GetMapping
    public ProfileResponse getProfile(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        ProfileData profile = jwt == null
                ? profileService.getProfile(username)
                : profileService.getProfile(username, AuthenticatedUser.idOf(jwt));
        return ProfileResponse.of(profile);
    }

    @PostMapping("/follow")
    public ProfileResponse follow(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.of(profileService.follow(AuthenticatedUser.idOf(jwt), username));
    }

    @DeleteMapping("/follow")
    public ProfileResponse unfollow(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.of(profileService.unfollow(AuthenticatedUser.idOf(jwt), username));
    }
}
