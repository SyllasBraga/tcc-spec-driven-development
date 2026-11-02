package com.example.blog.usuario.web;

import com.example.blog.usuario.service.UsuarioAutenticadoDto;

public record UsuarioEnvelope(UsuarioResponse user) {

    public static UsuarioEnvelope de(UsuarioAutenticadoDto dto) {
        return new UsuarioEnvelope(
                new UsuarioResponse(dto.email(), dto.token(), dto.username(), dto.bio(), dto.imagem()));
    }
}
