package com.krzelj.lms.security.jwt;

import com.krzelj.lms.domain.Role;
import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtServiceImpl jwtService;
    private String testSecret;
    private String testIssuer;

    @BeforeEach
    void setUp() {
        testSecret = "j430tj340utijfsifjgsoijjOIJXIWJXoiaj9019032i12keopkakwdwaopdk1i4912iopkqwodpwqk91'2i34";
        testIssuer = "lms-test";
        
        jwtService = new JwtServiceImpl(testSecret, testIssuer, 30);
    }

    @Test
    void generateAccessToken_ValidInput_ReturnsToken() {
        User user = new User("testuser", "hash", "testuser@test.com");
        user.setId(1L);
        user.setRoles(Set.of(new Role(RoleName.STUDENT)));

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void validateAccessToken_ValidToken_ReturnsClaims() {
        User user = new User("testuser", "hash", "testuser@test.com");
        user.setId(1L);
        user.setRoles(Set.of(new Role(RoleName.STUDENT), new Role(RoleName.INSTRUCTOR)));

        String token = jwtService.generateAccessToken(user);
        AccessTokenClaims validatedClaims = jwtService.validateAccessToken(token);

        assertNotNull(validatedClaims);
        assertEquals(1L, validatedClaims.userId());
        assertEquals("testuser", validatedClaims.username());
        assertEquals("testuser@test.com", validatedClaims.email());
        assertEquals(2, validatedClaims.roles().size());
        assertTrue(validatedClaims.roles().contains(RoleName.STUDENT));
        assertTrue(validatedClaims.roles().contains(RoleName.INSTRUCTOR));
    }

    @Test
    void validateAccessToken_InvalidToken_ThrowsException() {
        String invalidToken = "invalid.token.here";

        assertThrows(SecurityException.class, () -> jwtService.validateAccessToken(invalidToken));
    }

    @Test
    void validateAccessToken_TamperedToken_ThrowsException() {
        User user = new User("testuser", "hash", "testuser@test.com");
        user.setId(1L);
        user.setRoles(Set.of(new Role(RoleName.STUDENT)));

        String token = jwtService.generateAccessToken(user);
        String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

        assertThrows(SecurityException.class, () -> jwtService.validateAccessToken(tamperedToken));
    }

    @Test
    void generateRefreshToken_ReturnsToken() {
        String token = jwtService.generateRefreshToken();

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void hashToken_ReturnsHash() {
        String token = "test-token";
        String hash = jwtService.hashToken(token);

        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        assertEquals(hash, jwtService.hashToken(token));
    }

    @Test
    void getAccessTokenExpirationSeconds_ReturnsCorrectValue() {
        long expiration = jwtService.getAccessTokenExpirationSeconds();

        assertEquals(1800, expiration);
    }
}
