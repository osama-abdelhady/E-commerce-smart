package com.tailoredplatform.ecommerce.common.util;

import com.tailoredplatform.ecommerce.security.jwt.CookieProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The refresh token cookie is HttpOnly (invisible to JS — mitigates XSS
 * exfiltration), Secure (dev profile turns this off for http://localhost),
 * SameSite=Strict, and Path-scoped to /api/v1/auth so it is never sent on
 * ordinary API calls, only to login/refresh/logout. The raw token value
 * lives ONLY in this cookie; the database stores just its SHA-256 hash
 * (see RefreshToken entity / RefreshTokenService).
 */
@Component
@RequiredArgsConstructor
public class CookieUtil {

    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final CookieProperties cookieProperties;

    public ResponseCookie buildRefreshCookie(String rawToken, Duration maxAge) {
        return ResponseCookie.from(cookieProperties.refreshTokenName(), rawToken)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie buildExpiredRefreshCookie() {
        return ResponseCookie.from(cookieProperties.refreshTokenName(), "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    public String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (var cookie : request.getCookies()) {
            if (cookieProperties.refreshTokenName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
