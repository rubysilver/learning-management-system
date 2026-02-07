package com.krzelj.lms.security.jwt;

import com.krzelj.lms.domain.RoleName;

import java.util.Set;

public record AccessTokenClaims(
        long userId,
        String email,
        String username,
        Set<RoleName> roles
) {
}

