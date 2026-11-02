package com.example.blog.profile.web;

/** Perfil público no formato do contrato. */
public record ProfileBody(String username, String bio, String image, boolean following) {
}
