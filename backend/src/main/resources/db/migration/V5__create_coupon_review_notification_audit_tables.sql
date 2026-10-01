-- ============================================================
-- V5: Coupons, Reviews, Notifications, Audit
-- COUPONS, COUPON_REDEMPTIONS, REVIEWS, NOTIFICATIONS, AUDIT_LOGS
-- ============================================================

CREATE TABLE coupons (
    id                      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code                    VARCHAR2(50) NOT NULL,
    discount_type           VARCHAR2(20) NOT NULL,
    discount_value          NUMBER(12,2) NOT NULL,
    minimum_order_amount     NUMBER(12,2) DEFAULT 0 NOT NULL,
    max_redemptions          NUMBER,
    max_redemptions_per_user NUMBER DEFAULT 1 NOT NULL,
    redemptions_count        NUMBER DEFAULT 0 NOT NULL,
    valid_from               TIMESTAMP NOT NULL,
    valid_until               TIMESTAMP NOT NULL,
    is_active                NUMBER(1) DEFAULT 1 NOT NULL,
    created_at                TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_coupons_code UNIQUE (code),
    CONSTRAINT ck_coupons_discount_type CHECK (discount_type IN ('PERCENTAGE','FIXED_AMOUNT')),
    CONSTRAINT ck_coupons_discount_value CHECK (discount_value > 0),
    CONSTRAINT ck_coupons_is_active CHECK (is_active IN (0,1)),
    CONSTRAINT ck_coupons_validity CHECK (valid_until > valid_from)
);

CREATE INDEX idx_coupons_code_active ON coupons (code, is_active);

-- Enforces per-user redemption limits and gives a definitive audit trail of
-- which order used which coupon, at what discount amount, at redemption time.
CREATE TABLE coupon_redemptions (
    id                  NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    coupon_id           NUMBER NOT NULL,
    user_id             NUMBER NOT NULL,
    order_id            NUMBER NOT NULL,
    discount_applied     NUMBER(12,2) NOT NULL,
    redeemed_at          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_coupon_redemptions_order UNIQUE (order_id),
    CONSTRAINT fk_coupon_redemptions_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT fk_coupon_redemptions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_coupon_redemptions_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_coupon_redemptions_coupon_user ON coupon_redemptions (coupon_id, user_id);

ALTER TABLE orders ADD CONSTRAINT fk_orders_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id);

CREATE TABLE reviews (
    id                  NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id          NUMBER NOT NULL,
    user_id             NUMBER NOT NULL,
    order_item_id       NUMBER,
    rating              NUMBER(1) NOT NULL,
    title                VARCHAR2(150),
    body                 VARCHAR2(2000),
    status               VARCHAR2(20) DEFAULT 'PUBLISHED' NOT NULL,
    created_at            TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at            TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_status CHECK (status IN ('PUBLISHED','FLAGGED','REMOVED')),
    -- one review per purchased line item — prevents duplicate reviews for the same purchase
    CONSTRAINT uq_reviews_order_item UNIQUE (order_item_id)
);

CREATE INDEX idx_reviews_product ON reviews (product_id);
CREATE INDEX idx_reviews_user ON reviews (user_id);

CREATE TABLE notifications (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         NUMBER NOT NULL,
    type            VARCHAR2(50) NOT NULL,
    title            VARCHAR2(200) NOT NULL,
    body             VARCHAR2(1000),
    is_read          NUMBER(1) DEFAULT 0 NOT NULL,
    reference_type   VARCHAR2(30),
    reference_id     NUMBER,
    created_at        TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_notifications_is_read CHECK (is_read IN (0,1))
);

CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read);

-- Append-only. Every sensitive admin action (status change, refund, role
-- change, product deletion) writes one row here — never updated or deleted.
CREATE TABLE audit_logs (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_user_id   NUMBER,
    action           VARCHAR2(100) NOT NULL,
    entity_type      VARCHAR2(50) NOT NULL,
    entity_id        NUMBER,
    details_json      CLOB,
    ip_address        VARCHAR2(64),
    created_at        TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_user_id);
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at);
