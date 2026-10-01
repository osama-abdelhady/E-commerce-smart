package com.tailoredplatform.ecommerce.common;

import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.common.exception.GlobalExceptionHandler;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    void resourceNotFound_mapsTo404() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                ResourceNotFoundException.of("Order", 5L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).contains("Order").contains("5");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
    }

    @Test
    void businessRuleViolation_mapsTo409() {
        ResponseEntity<ApiErrorResponse> response = handler.handleBusinessRule(
                new BusinessRuleViolationException("Insufficient stock."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("BUSINESS_RULE_VIOLATION");
    }

    @Test
    void optimisticLockingFailure_mapsTo409_withARetryHint() {
        ResponseEntity<ApiErrorResponse> response = handler.handleOptimisticLock(
                new OptimisticLockingFailureException("stale"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(response.getBody().message()).containsIgnoringCase("retry");
    }

    @Test
    void badCredentials_mapsTo401_andNeverLeaksWhichFieldWasWrong() {
        ResponseEntity<ApiErrorResponse> response = handler.handleBadCredentials(
                new BadCredentialsException("bad"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Deliberately generic — never "no such user" vs "wrong password",
        // which would let an attacker enumerate valid emails.
        assertThat(response.getBody().message()).isEqualTo("Invalid email or password.");
    }

    @Test
    void disabledAccount_mapsTo403() {
        ResponseEntity<ApiErrorResponse> response = handler.handleDisabled(
                new DisabledException("locked out"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error()).isEqualTo("ACCOUNT_LOCKED");
    }

    @Test
    void accessDenied_mapsTo403() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("nope"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void unexpectedException_mapsTo500_andNeverLeaksTheRawExceptionMessage() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("some internal detail like a table name or stack frame"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).doesNotContain("table name");
    }
}
