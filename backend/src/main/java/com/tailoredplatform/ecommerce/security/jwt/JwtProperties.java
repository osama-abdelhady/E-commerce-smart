package com.tailoredplatform.ecommerce.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        int accessTokenExpirationMinutes,
        int refreshTokenExpirationDays,
        String issuer
) {
}
