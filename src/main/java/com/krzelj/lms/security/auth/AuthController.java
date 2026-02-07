package com.krzelj.lms.security.auth;

import com.krzelj.lms.security.auth.dto.AccessTokenResponse;
import com.krzelj.lms.security.auth.dto.LoginRequest;
import com.krzelj.lms.security.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AccessTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        var tokens = authService.register(request);
        return withRefreshCookie(tokens.refreshToken(), tokens.expiresIn(),
                ResponseEntity.status(HttpStatus.CREATED).body(new AccessTokenResponse(tokens.accessToken(), tokens.expiresIn())));
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        var tokens = authService.login(request);
        return withRefreshCookie(tokens.refreshToken(), tokens.expiresIn(),
                ResponseEntity.ok(new AccessTokenResponse(tokens.accessToken(), tokens.expiresIn())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(@CookieValue("refresh_token") String refreshToken) {
        var tokens = authService.refresh(refreshToken);
        return withRefreshCookie(tokens.refreshToken(), tokens.expiresIn(),
                ResponseEntity.ok(new AccessTokenResponse(tokens.accessToken(), tokens.expiresIn())));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = "refresh_token", required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false) // set true when using HTTPS
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private <T> ResponseEntity<T> withRefreshCookie(String refreshToken, long refreshMaxAgeSeconds, ResponseEntity<T> response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false) // set true when using HTTPS
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(authService.getRefreshTokenExpiration())
                .build();

        return ResponseEntity.status(response.getStatusCode())
                .headers(headers -> {
                    headers.addAll(response.getHeaders());
                    headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
                })
                .body(response.getBody());
    }
}

