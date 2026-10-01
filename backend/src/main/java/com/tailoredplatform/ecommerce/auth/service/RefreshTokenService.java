package com.tailoredplatform.ecommerce.auth.service;

import com.tailoredplatform.ecommerce.auth.entity.RefreshToken;
import com.tailoredplatform.ecommerce.auth.repository.RefreshTokenRepository;
import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.security.jwt.JwtProperties;
import com.tailoredplatform.ecommerce.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

/**
 * Refresh tokens are opaque random values, stored only as a SHA-256 hash
 * (never the raw value — that lives solely in the HttpOnly cookie). Every
 * successful refresh call ROTATES the token: the old row is marked revoked
 * and linked via replacedBy to the newly issued row. If a revoked token is
 * ever presented again, that's a signal of token theft/reuse — every
 * refresh token descended from that chain is revoked immediately.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public record IssuedToken(String rawToken, RefreshToken entity) {}

    @Transactional
    public IssuedToken issue(User user, String userAgent, String ipAddress) {
        String rawToken = generateRawToken();
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hash(rawToken));
        entity.setIssuedAt(Instant.now());
        entity.setExpiresAt(Instant.now().plus(jwtProperties.refreshTokenExpirationDays(), ChronoUnit.DAYS));
        entity.setUserAgent(truncate(userAgent, 255));
        entity.setIpAddress(ipAddress);
        refreshTokenRepository.save(entity);
        return new IssuedToken(rawToken, entity);
    }

    /**
     * Validates the presented raw token and rotates it. Throws if the token
     * is unknown, expired, or already revoked — a revoked-but-presented
     * token additionally triggers a full revocation of its rotation chain,
     * since that pattern only occurs if a stolen token is being replayed
     * after the legitimate client already rotated past it.
     */
    @Transactional
    public IssuedToken verifyAndRotate(String rawToken, String userAgent, String ipAddress) {
        String tokenHash = hash(rawToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid refresh token."));

        if (existing.getRevokedAt() != null) {
            revokeDescendants(existing);
            throw new BusinessRuleViolationException(
                    "Refresh token reuse detected. All sessions for this token chain have been revoked.");
        }

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleViolationException("Refresh token expired. Please log in again.");
        }

        IssuedToken next = issue(existing.getUser(), userAgent, ipAddress);
        existing.setRevokedAt(Instant.now());
        existing.setReplacedBy(next.entity());
        refreshTokenRepository.save(existing);

        return next;
    }

    @Transactional
    public void revoke(String rawToken) {
        Optional<RefreshToken> token = refreshTokenRepository.findByTokenHash(hash(rawToken));
        token.ifPresent(t -> {
            t.setRevokedAt(Instant.now());
            refreshTokenRepository.save(t);
        });
    }

    private void revokeDescendants(RefreshToken token) {
        RefreshToken current = token;
        while (current != null) {
            current.setRevokedAt(Instant.now());
            refreshTokenRepository.save(current);
            current = current.getReplacedBy();
        }
    }

    private String generateRawToken() {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(KeyGenerators.secureRandom(48).generateKey());
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
