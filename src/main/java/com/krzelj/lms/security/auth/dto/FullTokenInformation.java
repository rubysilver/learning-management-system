package com.krzelj.lms.security.auth.dto;

public record FullTokenInformation(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
    public static FullTokenInformation of(String accessToken, String refreshToken, long expiresIn) {
        return new FullTokenInformation(accessToken, refreshToken, expiresIn);
    }
}

