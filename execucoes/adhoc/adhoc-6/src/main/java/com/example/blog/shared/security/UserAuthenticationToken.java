package com.example.blog.shared.security;

import java.util.List;
import java.util.Objects;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Authentication created from a validated JWT, carrying an {@link AuthenticatedUser} as principal.
 */
class UserAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedUser principal;

    UserAuthenticationToken(AuthenticatedUser principal) {
        super(List.of());
        this.principal = Objects.requireNonNull(principal);
        setAuthenticated(true);
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return principal.token();
    }

    @Override
    public String getName() {
        return Long.toString(principal.id());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof UserAuthenticationToken token
                && super.equals(token)
                && principal.equals(token.principal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), principal);
    }
}
