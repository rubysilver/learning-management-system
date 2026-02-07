package com.krzelj.lms.security.auth;

import com.krzelj.lms.domain.RefreshToken;
import com.krzelj.lms.domain.Role;
import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.repository.RefreshTokenRepository;
import com.krzelj.lms.repository.RoleRepository;
import com.krzelj.lms.repository.UserRepository;
import com.krzelj.lms.security.auth.dto.FullTokenInformation;
import com.krzelj.lms.security.auth.dto.LoginRequest;
import com.krzelj.lms.security.auth.dto.RegisterRequest;
import com.krzelj.lms.security.jwt.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    private final Duration refreshTokenExpiration = Duration.ofDays(7);

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public FullTokenInformation register(
            RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthException("Email already registered");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new AuthException("Username already taken");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.username(), passwordHash, request.email());

        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.STUDENT)));
        user.getRoles().add(studentRole);

        user = userRepository.save(user);
        return createTokens(user);
    }

    @Override
    public FullTokenInformation login(LoginRequest request) {
        User user = userRepository.findByEmailOrUsername(request.identifier(), request.identifier())
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("Invalid credentials");
        }
        if (!user.isEnabled()) {
            throw new AuthException("Account is disabled");
        }

        refreshTokenRepository.deleteAllByUserId(user.getId());
        return createTokens(user);
    }

    @Override
    public FullTokenInformation refresh(String refreshToken) {
        String tokenHash = jwtService.hashToken(refreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (Instant.now().isAfter(stored.getExpiresAt())) {
            refreshTokenRepository.deleteById(stored.getId());
            throw new AuthException("Refresh token expired");
        }

        User user = userRepository.findById(stored.getUser().getId())
                .orElseThrow(() -> new AuthException("User not found"));

        if (!user.isEnabled()) {
            throw new AuthException("Account is disabled");
        }

        int deleted = refreshTokenRepository.deleteByTokenHashReturningCount(tokenHash);
        if (deleted == 0) {
            throw new AuthException("Token already used");
        }

        return createTokens(user);
    }

    @Override
    public void logout(String refreshToken) {
        String tokenHash = jwtService.hashToken(refreshToken);
        refreshTokenRepository.deleteByTokenHash(tokenHash);
    }

    @Override
    public Duration getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    private FullTokenInformation createTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();
        String refreshTokenHash = jwtService.hashToken(refreshToken);

        Instant expiresAt = Instant.now().plus(refreshTokenExpiration);
        refreshTokenRepository.save(new RefreshToken(user, refreshTokenHash, expiresAt));

        return FullTokenInformation.of(accessToken, refreshToken, jwtService.getAccessTokenExpirationSeconds());
    }
}

