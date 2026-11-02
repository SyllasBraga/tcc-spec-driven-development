package com.example.blog.profile.web;

import com.example.blog.profile.service.ProfileService;
import com.example.blog.shared.security.CurrentUserId;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perfis públicos e relação de seguir.
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUserId currentUserId;

    public ProfileController(ProfileService profileService, CurrentUserId currentUserId) {
        this.profileService = profileService;
        this.currentUserId = currentUserId;
    }

    @GetMapping("/{username}")
    public ProfileResponse buscar(@PathVariable String username) {
        return ProfileResponse.from(profileService.buscarPerfil(username, currentUserId.obterOuNulo()));
    }

    @PostMapping("/{username}/follow")
    public ProfileResponse seguir(@PathVariable String username) {
        return ProfileResponse.from(profileService.seguir(username, currentUserId.exigir()));
    }

    @DeleteMapping("/{username}/follow")
    public ProfileResponse deixarDeSeguir(@PathVariable String username) {
        return ProfileResponse.from(profileService.deixarDeSeguir(username, currentUserId.exigir()));
    }
}
