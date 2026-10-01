-- ============================================================
-- V4: Order, Payment & Shipment tables
-- ORDERS, ORDER_ITEMS, PAYMENTS, PAYMENT_TRANSACTIONS, SHIPMENTS
-- ============================================================

CREATE TABLE orders (
    id                      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_number            VARCHAR2(30) NOT NULL,
    user_id                 NUMBER NOT NULL,
    status                  VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    subtotal                NUMBER(12,2) NOT NULL,
    discount_total          NUMBER(12,2) DEFAULT 0 NOT NULL,
    shipping_fee            NUMBER(12,2) DEFAULT 0 NOT NULL,
    tax_total               NUMBER(12,2) DEFAULT 0 NOT NULL,
    grand_total             NUMBER(12,2) NOT NULL,
    currency                VARCHAR2(3) DEFAULT 'USD' NOT NULL,
    shipping_address_id     NUMBER NOT NULL,
    billing_address_id      NUMBER NOT NULL,
    coupon_id               NUMBER,
    idempotency_key         VARCHAR2(100),
    placed_at               TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    cancelled_at            TIMESTAMP,
    cancellation_reason     VARCHAR2(500),
    version                 NUMBER DEFAULT 0 NOT NULL,
    created_at              TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at              TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_orders_number UNIQUE (order_number),
    CONSTRAINT uq_orders_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_shipping_addr FOREIGN KEY (shipping_address_id) REFERENCES addresses (id),
    CONSTRAINT fk_orders_billing_addr FOREIGN KEY (billing_address_id) REFERENCES addresses (id),
    CONSTRAINT ck_orders_status CHECK (status IN
        ('PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED','REFUNDED')),
    CONSTRAINT ck_orders_totals CHECK (subtotal >= 0 AND grand_total >= 0)
);

CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_placed_at ON orders (placed_at);

-- Snapshots product_name and unit_price at order time: later catalog price
-- edits or renames must NEVER retroactively change a placed order.
CREATE TABLE order_items (
    id                          NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id                    NUMBER NOT NULL,
    product_variant_id          NUMBER NOT NULL,
    product_name_snapshot       VARCHAR2(255) NOT NULL,
    sku_snapshot                VARCHAR2(60) NOT NULL,
    variant_label_snapshot       VARCHAR2(100),
    unit_price_snapshot          NUMBER(12,2) NOT NULL,
    quantity                     NUMBER NOT NULL,
    line_total                   NUMBER(12,2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_price CHECK (unit_price_snapshot >= 0)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

-- idempotency_key guards against a duplicated Stripe webhook or a retried
-- client request creating two payment records for the same attempt.
CREATE TABLE payments (
    id                      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id                NUMBER NOT NULL,
    provider                VARCHAR2(30) DEFAULT 'STRIPE' NOT NULL,
    provider_payment_id     VARCHAR2(255),
    status                  VARCHAR2(20) DEFAULT 'INITIATED' NOT NULL,
    amount                  NUMBER(12,2) NOT NULL,
    currency                VARCHAR2(3) DEFAULT 'USD' NOT NULL,
    idempotency_key         VARCHAR2(100) NOT NULL,
    failure_reason          VARCHAR2(500),
    created_at              TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at              TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_payments_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT ck_payments_status CHECK (status IN
        ('INITIATED','REQUIRES_ACTION','SUCCEEDED','FAILED','REFUNDED','PARTIALLY_REFUNDED'))
);

CREATE INDEX idx_payments_order ON payments (order_id);
CREATE INDEX idx_payments_provider_payment ON payments (provider_payment_id);

-- Full audit trail of every provider event received for a payment (webhook
-- deliveries included), so a support investigation can reconstruct exactly
-- what Stripe told us and when — never mutate this table, only append.
CREATE TABLE payment_transactions (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id      NUMBER NOT NULL,
    event_type      VARCHAR2(50) NOT NULL,
    raw_payload      CLOB,
    provider_event_id VARCHAR2(255),
    created_at      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_payment_tx_provider_event UNIQUE (provider_event_id),
    CONSTRAINT fk_payment_tx_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE
);

CREATE INDEX idx_payment_tx_payment ON payment_transactions (payment_id);

CREATE TABLE shipments (
    id                      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id                NUMBER NOT NULL,
    carrier                 VARCHAR2(100),
    tracking_number         VARCHAR2(100),
    status                  VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    shipped_at               TIMESTAMP,
    delivered_at             TIMESTAMP,
    estimated_delivery_at    TIMESTAMP,
    CONSTRAINT uq_shipments_order UNIQUE (order_id),
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_shipments_status CHECK (status IN
        ('PENDING','LABEL_CREATED','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','RETURNED'))
);
