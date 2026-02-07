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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Role studentRole;

    @BeforeEach
    void setUp() {
        studentRole = new Role(RoleName.STUDENT);
        studentRole.setId(1L);

        testUser = new User("testuser", "hashed", "test@test.com");
        testUser.setId(1L);
        testUser.setEnabled(true);
        Set<Role> roles = new HashSet<>();
        roles.add(studentRole);
        testUser.setRoles(roles);
    }

    @Test
    void register_ValidRequest_ReturnsTokens() {
        RegisterRequest request = new RegisterRequest("new@test.com", "newuser", "password123");
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access");
        when(jwtService.generateRefreshToken()).thenReturn("refresh");
        when(jwtService.hashToken("refresh")).thenReturn("hash");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(1800L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        FullTokenInformation result = authService.register(request);

        assertNotNull(result);
        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());
        assertEquals(1800L, result.expiresIn());

        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_EmailExists_ThrowsAuthException() {
        RegisterRequest request = new RegisterRequest("existing@test.com", "newuser", "password123");
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThrows(AuthException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_UsernameExists_ThrowsAuthException() {
        RegisterRequest request = new RegisterRequest("new@test.com", "existinguser", "password123");
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(AuthException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_ValidCredentials_ReturnsTokens() {
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userRepository.findByEmailOrUsername("testuser", "testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(refreshTokenRepository.deleteAllByUserId(1L)).thenReturn(0);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access");
        when(jwtService.generateRefreshToken()).thenReturn("refresh");
        when(jwtService.hashToken("refresh")).thenReturn("hash");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(1800L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        FullTokenInformation result = authService.login(request);

        assertNotNull(result);
        assertEquals("access", result.accessToken());
        verify(refreshTokenRepository).deleteAllByUserId(1L);
    }

    @Test
    void login_UserNotFound_ThrowsAuthException() {
        LoginRequest request = new LoginRequest("unknown", "password");
        when(userRepository.findByEmailOrUsername("unknown", "unknown")).thenReturn(Optional.empty());

        assertThrows(AuthException.class, () -> authService.login(request));

        verify(refreshTokenRepository, never()).deleteAllByUserId(anyLong());
    }

    @Test
    void login_WrongPassword_ThrowsAuthException() {
        LoginRequest request = new LoginRequest("testuser", "wrong");
        when(userRepository.findByEmailOrUsername("testuser", "testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(AuthException.class, () -> authService.login(request));

        verify(refreshTokenRepository, never()).deleteAllByUserId(anyLong());
    }

    @Test
    void login_DisabledUser_ThrowsAuthException() {
        testUser.setEnabled(false);
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userRepository.findByEmailOrUsername("testuser", "testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

        assertThrows(AuthException.class, () -> authService.login(request));
    }

    @Test
    void refresh_ValidToken_ReturnsNewTokens() {
        RefreshToken stored = new RefreshToken(testUser, "hash", Instant.now().plusSeconds(3600));
        when(jwtService.hashToken("valid-refresh")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(stored));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.deleteByTokenHashReturningCount("hash")).thenReturn(1);
        when(jwtService.generateAccessToken(testUser)).thenReturn("new-access");
        when(jwtService.generateRefreshToken()).thenReturn("new-refresh");
        when(jwtService.hashToken("new-refresh")).thenReturn("new-hash");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(1800L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        FullTokenInformation result = authService.refresh("valid-refresh");

        assertNotNull(result);
        assertEquals("new-access", result.accessToken());
        verify(refreshTokenRepository).deleteByTokenHashReturningCount("hash");
    }

    @Test
    void refresh_InvalidToken_ThrowsAuthException() {
        when(jwtService.hashToken("invalid")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

        assertThrows(AuthException.class, () -> authService.refresh("invalid"));
    }

    @Test
    void refresh_ExpiredToken_ThrowsAuthException() {
        RefreshToken stored = org.mockito.Mockito.spy(new RefreshToken(testUser, "hash", Instant.now().minusSeconds(1)));
        when(stored.getId()).thenReturn(1L);
        when(jwtService.hashToken("expired")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(stored));

        assertThrows(AuthException.class, () -> authService.refresh("expired"));

        verify(refreshTokenRepository).deleteById(1L);
    }

    @Test
    void refresh_DisabledUser_ThrowsAuthException() {
        testUser.setEnabled(false);
        RefreshToken stored = new RefreshToken(testUser, "hash", Instant.now().plusSeconds(3600));
        when(jwtService.hashToken("valid")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(stored));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(AuthException.class, () -> authService.refresh("valid"));
    }

    @Test
    void logout_DeletesToken() {
        when(jwtService.hashToken("refresh")).thenReturn("hash");
        when(refreshTokenRepository.deleteByTokenHash("hash")).thenReturn(1);

        authService.logout("refresh");

        verify(refreshTokenRepository).deleteByTokenHash("hash");
    }

    @Test
    void getRefreshTokenExpiration_ReturnsDuration() {
        assertNotNull(authService.getRefreshTokenExpiration());
        assertEquals(7, authService.getRefreshTokenExpiration().toDays());
    }
}
