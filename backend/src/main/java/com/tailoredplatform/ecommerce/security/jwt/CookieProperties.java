package com.tailoredplatform.ecommerce.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cookies")
public record CookieProperties(
        String refreshTokenName,
        boolean secure,
        String sameSite
) {
}
