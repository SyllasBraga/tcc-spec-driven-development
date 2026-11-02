package com.example.blog.perfil.web;

import com.example.blog.perfil.service.PerfilDto;

public record PerfilEnvelope(PerfilResponse profile) {

    public static PerfilEnvelope de(PerfilDto dto) {
        return new PerfilEnvelope(new PerfilResponse(dto.username(), dto.bio(), dto.imagem(), dto.seguindo()));
    }
}
