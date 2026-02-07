package com.krzelj.lms.web.mvc;

import com.krzelj.lms.config.SecurityConfig;
import com.krzelj.lms.security.auth.AuthException;
import com.krzelj.lms.security.auth.AuthService;
import com.krzelj.lms.security.auth.dto.RegisterRequest;
import com.krzelj.lms.security.jwt.JwtService;
import com.krzelj.lms.web.api.ApiControllerTestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegisterController.class)
@Import({SecurityConfig.class, ApiControllerTestSecurityConfig.class})
@ActiveProfiles("test")
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void registerForm_ReturnsRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void register_Success_RedirectsToLogin() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(null);

        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("email", "new@test.com")
                        .param("password", "password123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void register_Failure_RedirectsToRegisterWithError() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new AuthException("Username already exists"));

        mockMvc.perform(post("/register")
                        .param("username", "existing")
                        .param("email", "ex@test.com")
                        .param("password", "password123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));

        verify(authService).register(any(RegisterRequest.class));
    }
}
