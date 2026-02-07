package com.krzelj.lms.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krzelj.lms.security.auth.dto.FullTokenInformation;
import com.krzelj.lms.config.SecurityConfig;
import com.krzelj.lms.security.auth.dto.LoginRequest;
import com.krzelj.lms.security.auth.dto.RegisterRequest;
import com.krzelj.lms.security.jwt.JwtService;
import com.krzelj.lms.web.api.ApiControllerTestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, ApiControllerTestSecurityConfig.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(authService.getRefreshTokenExpiration()).thenReturn(Duration.ofDays(7));
    }

    @Test
    void register_ValidRequest_ReturnsCreatedAndSetsCookie() throws Exception {
        RegisterRequest request = new RegisterRequest("user@test.com", "newuser", "password123");
        FullTokenInformation tokens = FullTokenInformation.of("access-token", "refresh-token", 1800L);
        when(authService.register(any(RegisterRequest.class))).thenReturn(tokens);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(header().exists("Set-Cookie"));

        verify(authService).register(any(RegisterRequest.class));
        verify(authService).getRefreshTokenExpiration();
    }

    @Test
    void login_ValidRequest_ReturnsOkAndSetsCookie() throws Exception {
        LoginRequest request = new LoginRequest("user@test.com", "password123");
        FullTokenInformation tokens = FullTokenInformation.of("access-token", "refresh-token", 1800L);
        when(authService.login(any(LoginRequest.class))).thenReturn(tokens);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(header().exists("Set-Cookie"));

        verify(authService).login(any(LoginRequest.class));
        verify(authService).getRefreshTokenExpiration();
    }

    @Test
    void refresh_WithValidCookie_ReturnsOkAndNewTokens() throws Exception {
        FullTokenInformation tokens = FullTokenInformation.of("new-access", "new-refresh", 1800L);
        when(authService.refresh("valid-refresh-token")).thenReturn(tokens);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(header().exists("Set-Cookie"));

        verify(authService).refresh("valid-refresh-token");
    }

    @Test
    void logout_WithCookie_ClearsCookieAndReturnsNoContent() throws Exception {
        doNothing().when(authService).logout("refresh-token-value");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-token-value")))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"));

        verify(authService).logout("refresh-token-value");
    }

    @Test
    void logout_WithoutCookie_ReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());

        verify(authService, never()).logout(any());
    }
}
