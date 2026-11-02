package com.example.blog.profile.web;

import com.example.blog.profile.service.ProfileService;
import com.example.blog.profile.service.ProfileView;
import com.example.blog.shared.security.AuthenticatedUser;
import com.example.blog.shared.web.ApiMediaTypes;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Perfis públicos e relações de "seguir". */
@RestController
@RequestMapping(path = "/api/profiles/{username}", produces = ApiMediaTypes.JSON_UTF8_VALUE)
class ProfileController {

    private final ProfileService profileService;

    ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /** Autenticação opcional: sem token, {@code jwt} é nulo e {@code following} sai falso. */
    @GetMapping
    ProfileResponse getProfile(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        ProfileView view = jwt == null
                ? profileService.getProfile(username)
                : profileService.getProfile(username, AuthenticatedUser.from(jwt).id());
        return ProfileResponse.from(view);
    }

    @PostMapping("/follow")
    ProfileResponse follow(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.from(profileService.follow(AuthenticatedUser.from(jwt).id(), username));
    }

    @DeleteMapping("/follow")
    ProfileResponse unfollow(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.from(profileService.unfollow(AuthenticatedUser.from(jwt).id(), username));
    }
}
