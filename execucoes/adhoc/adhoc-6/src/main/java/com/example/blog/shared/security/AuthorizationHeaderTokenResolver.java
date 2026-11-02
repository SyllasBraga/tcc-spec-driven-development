package com.example.blog.shared.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Reads the token from {@code Authorization: Token <jwt>}, the scheme required by the API contract.
 * The {@code Bearer} scheme is also accepted. Other schemes are ignored, as if no token had been sent.
 */
public class AuthorizationHeaderTokenResolver implements BearerTokenResolver {

    private static final String TOKEN_SCHEME = "Token ";
    private static final String BEARER_SCHEME = "Bearer ";

    @Override
    public String resolve(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null) {
            return null;
        }
        if (startsWithIgnoringCase(header, TOKEN_SCHEME)) {
            return tokenAfter(header, TOKEN_SCHEME);
        }
        if (startsWithIgnoringCase(header, BEARER_SCHEME)) {
            return tokenAfter(header, BEARER_SCHEME);
        }
        return null;
    }

    private static boolean startsWithIgnoringCase(String header, String scheme) {
        return header.regionMatches(true, 0, scheme, 0, scheme.length());
    }

    private static String tokenAfter(String header, String scheme) {
        String token = header.substring(scheme.length()).trim();
        if (token.isEmpty()) {
            throw new InvalidBearerTokenException("Token is empty");
        }
        return token;
    }
}
