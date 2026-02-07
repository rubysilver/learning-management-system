package com.krzelj.lms.security.auth.dto;

public record AccessTokenResponse(
        String accessToken,
        long expiresIn
) {
}

