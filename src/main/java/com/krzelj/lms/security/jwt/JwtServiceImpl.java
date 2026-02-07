package com.krzelj.lms.security.jwt;

import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class JwtServiceImpl implements JwtService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey secretKey;
    private final Duration accessTokenExpiration;
    private final String issuer;

    public JwtServiceImpl(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.issuer:lms}") String issuer,
            @Value("${app.jwt.access-token-minutes:30}") long accessTokenMinutes
    ) {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters for HS256");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
        this.issuer = issuer;
        this.accessTokenExpiration = Duration.ofMinutes(accessTokenMinutes);
    }

    @Override
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpiration);

        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .toList();

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(issuer)
                .claim("email", user.getEmail())
                .claim("username", user.getUsername())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(java.util.UUID.randomUUID().toString())
                .signWith(secretKey)
                .compact();
    }

    @Override
    public AccessTokenClaims validateAccessToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .requireIssuer(issuer)
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jws.getPayload();
            long userId = Long.parseLong(claims.getSubject());
            String email = claims.get("email", String.class);
            String username = claims.get("username", String.class);

            Set<RoleName> roles = new HashSet<>();
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List<?> list) {
                for (Object r : list) {
                    if (r != null) roles.add(RoleName.valueOf(String.valueOf(r)));
                }
            }

            return new AccessTokenClaims(userId, email, username, roles);
        } catch (Exception e) {
            throw new SecurityException("Invalid token", e);
        }
    }

    @Override
    public String generateRefreshToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Override
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Override
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration.toSeconds();
    }
}

