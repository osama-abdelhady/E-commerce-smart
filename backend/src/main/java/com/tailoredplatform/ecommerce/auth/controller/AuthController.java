package com.tailoredplatform.ecommerce.auth.controller;

import com.tailoredplatform.ecommerce.auth.dto.AuthResponse;
import com.tailoredplatform.ecommerce.auth.dto.ChangePasswordRequest;
import com.tailoredplatform.ecommerce.auth.dto.ForgotPasswordRequest;
import com.tailoredplatform.ecommerce.auth.dto.LoginRequest;
import com.tailoredplatform.ecommerce.auth.dto.RegisterRequest;
import com.tailoredplatform.ecommerce.auth.dto.ResetPasswordRequest;
import com.tailoredplatform.ecommerce.auth.service.AuthService;
import com.tailoredplatform.ecommerce.common.util.CookieUtil;
import com.tailoredplatform.ecommerce.security.UserPrincipal;
import com.tailoredplatform.ecommerce.security.jwt.JwtProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, token refresh, and password management")
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        var result = authService.register(request, httpRequest.getHeader(HttpHeaders.USER_AGENT), httpRequest.getRemoteAddr());
        return withRefreshCookie(result.response(), result.rawRefreshToken(), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive an access token; refresh token is set as an HttpOnly cookie")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        var result = authService.login(request, httpRequest.getHeader(HttpHeaders.USER_AGENT), httpRequest.getRemoteAddr());
        return withRefreshCookie(result.response(), result.rawRefreshToken(), HttpStatus.OK);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange the refresh-token cookie for a new access token (rotates the refresh token)")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest httpRequest) {
        String rawToken = cookieUtil.readRefreshCookie(httpRequest);
        var result = authService.refresh(rawToken, httpRequest.getHeader(HttpHeaders.USER_AGENT), httpRequest.getRemoteAddr());
        return withRefreshCookie(result.response(), result.rawRefreshToken(), HttpStatus.OK);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current refresh token and clear its cookie")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        String rawToken = cookieUtil.readRefreshCookie(httpRequest);
        authService.logout(rawToken);
        ResponseCookie expired = cookieUtil.buildExpiredRefreshCookie();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expired.toString())
                .build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset email (always returns 202, regardless of whether the email exists)")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using the token emailed by /forgot-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password for the currently authenticated user")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(principal.getId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address using the token emailed at registration")
    public ResponseEntity<Void> verifyEmail(@RequestBody String token) {
        authService.verifyEmail(token);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthResponse response, String rawRefreshToken, HttpStatus status) {
        Duration maxAge = Duration.ofDays(jwtProperties.refreshTokenExpirationDays());
        ResponseCookie cookie = cookieUtil.buildRefreshCookie(rawRefreshToken, maxAge);
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}
