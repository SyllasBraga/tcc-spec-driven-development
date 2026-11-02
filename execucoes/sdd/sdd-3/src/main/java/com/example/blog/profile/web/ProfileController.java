package com.example.blog.profile.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.profile.service.Perfil;
import com.example.blog.profile.service.ProfileService;
import com.example.blog.shared.security.UsuarioAtual;
import com.example.blog.shared.web.MediaTypes;

/**
 * Perfil público e seguir/deixar de seguir.
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final UsuarioAtual usuarioAtual;

    public ProfileController(ProfileService profileService, UsuarioAtual usuarioAtual) {
        this.profileService = profileService;
        this.usuarioAtual = usuarioAtual;
    }

    @GetMapping(path = "/{username}", produces = MediaTypes.APPLICATION_JSON_UTF8)
    public ProfileResponse buscar(@PathVariable String username) {
        return resposta(profileService.buscar(username, usuarioAtual.idOpcional()));
    }

    @PostMapping(path = "/{username}/follow", produces = MediaTypes.APPLICATION_JSON_UTF8)
    public ProfileResponse seguir(@PathVariable String username) {
        return resposta(profileService.seguir(usuarioAtual.idObrigatorio(), username));
    }

    @DeleteMapping(path = "/{username}/follow", produces = MediaTypes.APPLICATION_JSON_UTF8)
    public ProfileResponse deixarDeSeguir(@PathVariable String username) {
        return resposta(profileService.deixarDeSeguir(usuarioAtual.idObrigatorio(), username));
    }

    private static ProfileResponse resposta(Perfil perfil) {
        return new ProfileResponse(ProfileView.de(perfil));
    }
}
