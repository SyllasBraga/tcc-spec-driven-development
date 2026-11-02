package com.example.blog.user.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.shared.security.AuthenticatedUser;
import com.example.blog.user.service.ProfileData;
import com.example.blog.user.service.ProfileService;

/**
 * Public profiles and following ({@code /api/profiles}).
 */
@RestController
@RequestMapping("/api/profiles/{username}")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /** Authentication is optional; without it {@code following} is always false. */
    @GetMapping
    public ProfileResponse getProfile(@PathVariable String username,
            @AuthenticationPrincipal AuthenticatedUser viewer) {
        ProfileData profile = viewer == null
                ? profileService.getProfile(username)
                : profileService.getProfile(username, viewer.id());
        return toResponse(profile);
    }

    @PostMapping("/follow")
    public ProfileResponse follow(@PathVariable String username,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return toResponse(profileService.follow(principal.id(), username));
    }

    @DeleteMapping("/follow")
    public ProfileResponse unfollow(@PathVariable String username,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return toResponse(profileService.unfollow(principal.id(), username));
    }

    private static ProfileResponse toResponse(ProfileData profile) {
        return new ProfileResponse(ProfileView.of(profile));
    }
}
