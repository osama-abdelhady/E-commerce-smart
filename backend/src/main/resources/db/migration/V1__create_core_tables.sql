-- ============================================================
-- V1: Core identity tables
-- USERS, ROLES, USER_ROLES, ADDRESSES, REFRESH_TOKENS
-- ============================================================

CREATE TABLE roles (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR2(50) NOT NULL,
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE users (
    id                  NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email               VARCHAR2(255) NOT NULL,
    password_hash       VARCHAR2(255) NOT NULL,
    full_name           VARCHAR2(150) NOT NULL,
    phone               VARCHAR2(30),
    status              VARCHAR2(20) DEFAULT 'PENDING_VERIFICATION' NOT NULL,
    email_verified_at   TIMESTAMP,
    failed_login_count  NUMBER(3) DEFAULT 0 NOT NULL,
    locked_until        TIMESTAMP,
    version             NUMBER DEFAULT 0 NOT NULL,
    created_at          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','SUSPENDED','DEACTIVATED'))
);

CREATE INDEX idx_users_status ON users (status);

CREATE TABLE user_roles (
    user_id     NUMBER NOT NULL,
    role_id     NUMBER NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE TABLE addresses (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         NUMBER NOT NULL,
    label           VARCHAR2(50),
    full_name       VARCHAR2(150) NOT NULL,
    line1           VARCHAR2(255) NOT NULL,
    line2           VARCHAR2(255),
    city            VARCHAR2(100) NOT NULL,
    state           VARCHAR2(100),
    postal_code     VARCHAR2(20) NOT NULL,
    country         VARCHAR2(2) NOT NULL,
    phone           VARCHAR2(30) NOT NULL,
    is_default      NUMBER(1) DEFAULT 0 NOT NULL,
    created_at      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_addresses_is_default CHECK (is_default IN (0,1))
);

CREATE INDEX idx_addresses_user ON addresses (user_id);

-- Refresh tokens are stored HASHED (SHA-256 of the raw token); the raw value
-- only ever lives in the HttpOnly cookie on the client. Rotation on every
-- refresh call: consuming a token sets revoked_at and issues a fresh row.
CREATE TABLE refresh_tokens (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         NUMBER NOT NULL,
    token_hash      VARCHAR2(128) NOT NULL,
    issued_at       TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    revoked_at      TIMESTAMP,
    replaced_by_id  NUMBER,
    user_agent      VARCHAR2(255),
    ip_address      VARCHAR2(64),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_replaced FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens (id)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);
