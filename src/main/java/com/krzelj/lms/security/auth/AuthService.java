package com.krzelj.lms.security.auth;

import com.krzelj.lms.security.auth.dto.FullTokenInformation;
import com.krzelj.lms.security.auth.dto.LoginRequest;
import com.krzelj.lms.security.auth.dto.RegisterRequest;

import java.time.Duration;

public interface AuthService {
    FullTokenInformation register(RegisterRequest request);

    FullTokenInformation login(LoginRequest request);

    FullTokenInformation refresh(String refreshToken);

    void logout(String refreshToken);

    Duration getRefreshTokenExpiration();
}

