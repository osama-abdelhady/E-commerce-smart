package com.tailoredplatform.ecommerce.auth.dto;

import com.tailoredplatform.ecommerce.users.dto.UserResponse;

/**
 * The refresh token is deliberately NOT in this body — it's set as an
 * HttpOnly cookie by the controller. Only the short-lived access token
 * (kept in memory by the Angular client) and the user's profile are
 * returned here.
 */
public record AuthResponse(
        String accessToken,
        long expiresInSeconds,
        UserResponse user
) {
}
