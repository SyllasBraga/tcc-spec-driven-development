package com.example.blog.profile.web;

import com.example.blog.profile.service.ProfileData;
import com.example.blog.profile.service.ProfileService;
import com.example.blog.shared.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Perfis públicos e relação de seguir. */
@RestController
@RequestMapping("/api/profiles/{username}")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /** Autenticação opcional: com token válido, {@code following} reflete a relação com o usuário autenticado. */
    @GetMapping
    ProfileResponse getProfile(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        ProfileData profile = jwt == null
                ? profileService.getProfile(username)
                : profileService.getProfile(username, AuthenticatedUser.from(jwt).id());
        return ProfileResponse.of(profile);
    }

    @PostMapping("/follow")
    ProfileResponse follow(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.of(profileService.follow(AuthenticatedUser.from(jwt).id(), username));
    }

    @DeleteMapping("/follow")
    ProfileResponse unfollow(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.of(profileService.unfollow(AuthenticatedUser.from(jwt).id(), username));
    }
}
