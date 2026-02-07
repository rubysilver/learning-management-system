package com.krzelj.lms.security.jwt;

import com.krzelj.lms.domain.User;

public interface JwtService {
    String generateAccessToken(User user);

    AccessTokenClaims validateAccessToken(String token);

    String generateRefreshToken();

    String hashToken(String token);

    long getAccessTokenExpirationSeconds();
}

