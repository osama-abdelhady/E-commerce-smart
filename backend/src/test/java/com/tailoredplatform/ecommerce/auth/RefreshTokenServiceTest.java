package com.tailoredplatform.ecommerce.auth;

import com.tailoredplatform.ecommerce.auth.entity.RefreshToken;
import com.tailoredplatform.ecommerce.auth.repository.RefreshTokenRepository;
import com.tailoredplatform.ecommerce.auth.service.RefreshTokenService;
import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.security.jwt.JwtProperties;
import com.tailoredplatform.ecommerce.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties("test-secret-value-not-real", 15, 30, "test-issuer");
        service = new RefreshTokenService(refreshTokenRepository, jwtProperties);
        user = new User();
        user.setId(1L);
        // save() returns whatever was passed in, mimicking JpaRepository's real behavior for these tests
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void issue_storesOnlyAHashOfTheRawToken_neverTheRawValueItself() {
        var issued = service.issue(user, "test-agent", "127.0.0.1");

        assertThat(issued.rawToken()).isNotBlank();
        assertThat(issued.entity().getTokenHash()).isNotEqualTo(issued.rawToken());
        assertThat(issued.entity().getUser()).isEqualTo(user);
    }

    @Test
    void verifyAndRotate_rotatesTheToken_andReturnsANewOne() {
        var issued = service.issue(user, "agent", "ip");
        RefreshToken stored = issued.entity();
        when(refreshTokenRepository.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));

        var rotated = service.verifyAndRotate(issued.rawToken(), "agent", "ip");

        assertThat(stored.getRevokedAt()).isNotNull(); // old token is revoked
        assertThat(stored.getReplacedBy()).isEqualTo(rotated.entity()); // linked to its replacement
        assertThat(rotated.rawToken()).isNotEqualTo(issued.rawToken()); // a genuinely new token
    }

    @Test
    void verifyAndRotate_throws_forAnUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyAndRotate("some-raw-token", "agent", "ip"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void verifyAndRotate_throws_forAnExpiredToken() {
        var issued = service.issue(user, "agent", "ip");
        issued.entity().setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(refreshTokenRepository.findByTokenHash(issued.entity().getTokenHash())).thenReturn(Optional.of(issued.entity()));

        assertThatThrownBy(() -> service.verifyAndRotate(issued.rawToken(), "agent", "ip"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("expired");
    }

    /**
     * The core theft-detection guarantee: if a token that was ALREADY
     * rotated away (revoked) gets presented again — which only happens if
     * someone is replaying a stolen copy after the legitimate client moved
     * on — the entire chain descending from it must be revoked, not just
     * this one token.
     */
    @Test
    void verifyAndRotate_revokesTheEntireChain_whenARevokedTokenIsReplayed() {
        RefreshToken original = mock(RefreshToken.class);
        RefreshToken next = mock(RefreshToken.class);

        when(original.getRevokedAt()).thenReturn(Instant.now().minus(1, ChronoUnit.HOURS));
        when(original.getReplacedBy()).thenReturn(next);
        when(next.getReplacedBy()).thenReturn(null);

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(original));

        assertThatThrownBy(() -> service.verifyAndRotate("stolen-raw-token", "agent", "ip"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("reuse detected");

        verify(original, times(1)).setRevokedAt(any());
        verify(next, times(1)).setRevokedAt(any());
        // both the replayed token and everything descended from it get saved as revoked
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }
}
