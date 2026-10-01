package com.tailoredplatform.ecommerce.auth.service;

import com.tailoredplatform.ecommerce.auth.dto.AuthResponse;
import com.tailoredplatform.ecommerce.auth.dto.LoginRequest;
import com.tailoredplatform.ecommerce.auth.dto.RegisterRequest;
import com.tailoredplatform.ecommerce.auth.entity.EmailVerificationToken;
import com.tailoredplatform.ecommerce.auth.entity.PasswordResetToken;
import com.tailoredplatform.ecommerce.auth.repository.EmailVerificationTokenRepository;
import com.tailoredplatform.ecommerce.auth.repository.PasswordResetTokenRepository;
import com.tailoredplatform.ecommerce.cart.entity.Cart;
import com.tailoredplatform.ecommerce.cart.repository.CartRepository;
import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.notifications.service.EmailService;
import com.tailoredplatform.ecommerce.security.jwt.JwtProperties;
import com.tailoredplatform.ecommerce.security.jwt.JwtService;
import com.tailoredplatform.ecommerce.users.entity.Role;
import com.tailoredplatform.ecommerce.users.entity.User;
import com.tailoredplatform.ecommerce.users.mapper.UserMapper;
import com.tailoredplatform.ecommerce.users.repository.RoleRepository;
import com.tailoredplatform.ecommerce.users.repository.UserRepository;
import com.tailoredplatform.ecommerce.wishlist.entity.Wishlist;
import com.tailoredplatform.ecommerce.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int EMAIL_VERIFICATION_EXPIRY_HOURS = 24;
    private static final int PASSWORD_RESET_EXPIRY_MINUTES = 30;
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final UserMapper userMapper;

    public record LoginResult(AuthResponse response, String rawRefreshToken) {}

    @Transactional
    public LoginResult register(RegisterRequest request, String userAgent, String ipAddress) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleViolationException("An account with this email already exists.");
        }

        Role customerRole = roleRepository.findByName(Role.Name.CUSTOMER.name())
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role missing — check V1 seed data."));

        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setStatus(User.Status.PENDING_VERIFICATION);
        user.getRoles().add(customerRole);
        userRepository.save(user);

        // Every new customer gets an empty cart and wishlist up front —
        // simplifies the cart/wishlist services (never need to lazily create one).
        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.save(cart);

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlistRepository.save(wishlist);

        issueEmailVerificationToken(user);

        return loginInternal(user, userAgent, ipAddress);
    }

    @Transactional
    public LoginResult login(LoginRequest request, String userAgent, String ipAddress) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new DisabledException("Account temporarily locked due to repeated failed login attempts. Try again later.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        } catch (BadCredentialsException ex) {
            registerFailedLogin(user);
            throw ex;
        }

        if (user.getFailedLoginCount() > 0) {
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        return loginInternal(user, userAgent, ipAddress);
    }

    @Transactional
    public LoginResult refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessRuleViolationException("No refresh token presented.");
        }
        RefreshTokenService.IssuedToken rotated = refreshTokenService.verifyAndRotate(rawRefreshToken, userAgent, ipAddress);
        User user = rotated.entity().getUser();
        AuthResponse response = buildAuthResponse(user);
        return new LoginResult(response, rotated.rawToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email.toLowerCase()).ifPresent(user -> {
            String rawToken = generateOpaqueToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(hash(rawToken));
            token.setExpiresAt(Instant.now().plus(PASSWORD_RESET_EXPIRY_MINUTES, ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(token);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), rawToken);
        });
        // Deliberately no branch for "email not found" — responding identically
        // either way avoids leaking which emails have accounts.
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid or expired reset token."));

        if (!token.isUsable()) {
            throw new BusinessRuleViolationException("Invalid or expired reset token.");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessRuleViolationException("Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid or expired verification token."));

        if (!token.isUsable()) {
            throw new BusinessRuleViolationException("Invalid or expired verification token.");
        }

        User user = token.getUser();
        user.setEmailVerifiedAt(Instant.now());
        if (user.getStatus() == User.Status.PENDING_VERIFICATION) {
            user.setStatus(User.Status.ACTIVE);
        }
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        emailVerificationTokenRepository.save(token);
    }

    // ---- internal helpers ----

    private LoginResult loginInternal(User user, String userAgent, String ipAddress) {
        AuthResponse response = buildAuthResponse(user);
        RefreshTokenService.IssuedToken refresh = refreshTokenService.issue(user, userAgent, ipAddress);
        return new LoginResult(response, refresh.rawToken());
    }

    private AuthResponse buildAuthResponse(User user) {
        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roleNames);
        long expiresInSeconds = jwtProperties.accessTokenExpirationMinutes() * 60L;
        return new AuthResponse(accessToken, expiresInSeconds, userMapper.toResponse(user));
    }

    private void registerFailedLogin(User user) {
        int attempts = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(attempts);
        if (attempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
        }
        userRepository.save(user);
    }

    private void issueEmailVerificationToken(User user) {
        String rawToken = generateOpaqueToken();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plus(EMAIL_VERIFICATION_EXPIRY_HOURS, ChronoUnit.HOURS));
        emailVerificationTokenRepository.save(token);
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), rawToken);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
