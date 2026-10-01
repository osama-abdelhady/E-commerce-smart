# Security Review

Written as part of Phase 9. Covers what was deliberately implemented for
security, what was checked, and what is explicitly out of scope for this
build — per deliverable #15 in the project spec ("Security considerations
and known limitations").

## Authentication & session management

- **Password hashing**: BCrypt, strength 10 (`SecurityConfig.passwordEncoder()`).
  Not Argon2 — BCrypt was chosen as the more universally-supported option;
  Argon2 would need `spring-security-crypto`'s `Argon2PasswordEncoder` swapped
  in, a one-line change if required later.
- **Access tokens**: short-lived (15 min default) JWTs, HMAC-signed, carried
  only in the `Authorization` header — never persisted client-side
  (`AuthService`, frontend, keeps it in an in-memory signal only). Verified:
  `JwtAuthenticationFilter` never reads a token from a cookie.
- **Refresh tokens**: opaque random values, stored server-side as a SHA-256
  hash only — the raw value exists solely in an HttpOnly, SameSite=Strict
  cookie, path-scoped to `/api/v1/auth`. Verified: `RefreshToken.tokenHash`
  is unique in the DB; the raw value is never persisted anywhere.
- **Refresh token rotation & theft detection**: every `/auth/refresh` call
  issues a new token and revokes the old one (`RefreshTokenService`). A
  revoked token presented again — only possible via a replayed stolen copy —
  revokes the entire descendant chain. Covered by
  `RefreshTokenServiceTest.verifyAndRotate_revokesTheEntireChain_whenARevokedTokenIsReplayed`.
- **Account lockout**: 5 failed login attempts locks the account for 15
  minutes (`AuthService.registerFailedLogin`). The lockout uses a generic 403
  (`ACCOUNT_LOCKED`), distinct from the generic 401 for bad-credentials — but
  see the user-enumeration note below.
- **Password reset / email verification tokens**: same hash-only-at-rest
  pattern as refresh tokens (`V7` migration). `forgot-password` always
  returns 202 regardless of whether the email exists.

## Authorization

- **Defense in depth on every admin endpoint**: both `SecurityConfig`'s
  `/api/v1/admin/**` URL matcher AND `@PreAuthorize("hasRole('ADMIN')")` on
  each admin controller. Verified end-to-end (not mocked) by
  `SecurityAuthorizationIT` — a real unauthenticated request gets 401, a real
  authenticated non-admin gets 403, a real admin gets 200, through the actual
  Spring Security filter chain.
- **Resource ownership checks are explicit, not implicit**: `OrderService`,
  `AddressService`, `CartService`, `WishlistService`, `ReviewService` all
  check `resource.getUser().getId().equals(currentUser.getId())` before
  allowing access — Spring Security's role check alone would let any
  authenticated customer view anyone's order by guessing an ID otherwise.

## CSRF

- Scoped deliberately to exactly the two cookie-authenticated endpoints
  (`/auth/refresh`, `/auth/logout`) — everything else is Bearer-token
  stateless and CSRF-exempt by design, since CSRF only matters where the
  browser attaches credentials (cookies) automatically. Verified by
  `SecurityAuthorizationIT`: posting to `/auth/refresh` without a CSRF token
  is rejected (403 from the CSRF filter specifically, not a business-logic
  error), and posting with a valid token gets past CSRF and fails for a
  business reason instead.

## Payments

- **Stripe signature verification is the only path that marks a payment
  succeeded** — `PaymentService.handleWebhook()` calls
  `Webhook.constructEvent()` before touching any payload data. A frontend
  "success" redirect is never trusted, per the spec's explicit requirement.
- **Webhook idempotency**: `PaymentTransaction.providerEventId` is unique;
  a re-delivered webhook is detected and skipped, not re-applied.
- **No raw card data ever reaches this backend** — Stripe Elements (frontend,
  not yet wired — see Phase 7/8 known limitations) handles card input
  directly with Stripe; this backend only ever sees a `PaymentIntent` ID.

## Input validation & injection

- Every request DTO uses Bean Validation (`jakarta.validation`) annotations;
  `GlobalExceptionHandler.handleValidation` returns field-level errors.
- All persistence goes through Spring Data JPA / Hibernate parameterized
  queries — no string-concatenated SQL anywhere in this codebase.
- Mass-assignment is structurally prevented: controllers only ever bind to
  explicit DTOs (`ProductCreateRequest`, `CheckoutRequest`, etc.), never
  directly to `@Entity` classes.

## What's explicitly NOT done (known gaps for a future pass)

- **No rate limiting** on `/auth/login`, `/auth/forgot-password`, or any
  other endpoint. Account lockout after 5 failed attempts provides some
  protection specifically against brute force, but nothing throttles
  registration spam, password-reset-email spam, or general API abuse.
  Bucket4j or a gateway-level solution (nginx `limit_req`, an API gateway)
  would be the next step.
- **Login timing may allow user enumeration**: `AuthService.login()` looks up
  the user, then calls `authenticationManager.authenticate()` — an
  unknown-email request short-circuits before BCrypt is invoked, while a
  known-email-wrong-password request pays BCrypt's cost, creating a timing
  side-channel that could reveal which emails have accounts. Mitigation would
  be running a dummy BCrypt comparison on the unknown-email path to equalize
  timing.
- **No audit log review tooling** — `AuditLog` rows are written correctly for
  order status changes and cancellations, but there's no endpoint to actually
  browse them yet (`AuditLogRepository` exists with query methods, unused by
  any controller).
- **CORS allowed origins is a single configurable string**, not validated
  against a strict allowlist format — fine for the single frontend origin
  this is built for, but worth tightening before adding a second frontend
  origin.
- **No dependency vulnerability scanning wired into the build** (no OWASP
  Dependency-Check / Snyk / `npm audit` in CI yet — CI itself is Phase 10).
- **Secrets in `.env.example` are placeholders**, but nothing enforces that a
  deployment actually sets strong values for `JWT_SECRET`/DB
  passwords/Stripe keys beyond `JwtService`'s startup check that
  `app.jwt.secret` isn't blank. There's no minimum-entropy check.

## Test coverage supporting this review

- `RefreshTokenServiceTest` — rotation and theft-detection logic
- `SecurityAuthorizationIT` — real filter chain, 401/403/200 gating, CSRF scoping
- `GlobalExceptionHandlerTest` — every security-relevant exception maps to
  the right status and never leaks internal detail
- `InventoryConcurrencyIT` — primarily a correctness test, but also covers a
  security-adjacent concern: a race condition letting two customers both
  "win" the last unit of stock would be a business-integrity bug, not just a
  UX one
