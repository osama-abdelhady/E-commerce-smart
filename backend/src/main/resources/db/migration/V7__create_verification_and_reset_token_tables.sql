-- ============================================================
-- V7: Email verification & password reset tokens
-- Added in Phase 3 (auth) — not foreseen in the original V1-V6 set.
-- Tokens are stored HASHED, same pattern as refresh_tokens: the raw
-- value is only ever emailed to the user, never persisted.
-- ============================================================

CREATE TABLE email_verification_tokens (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         NUMBER NOT NULL,
    token_hash      VARCHAR2(128) NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    used_at         TIMESTAMP,
    created_at      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_email_verif_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_verif_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_email_verif_user ON email_verification_tokens (user_id);

CREATE TABLE password_reset_tokens (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         NUMBER NOT NULL,
    token_hash      VARCHAR2(128) NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    used_at         TIMESTAMP,
    created_at      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_password_reset_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_user ON password_reset_tokens (user_id);
