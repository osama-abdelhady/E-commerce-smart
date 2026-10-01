-- ============================================================
-- V3: Cart & Wishlist tables
-- CARTS, CART_ITEMS, WISHLISTS, WISHLIST_ITEMS
-- ============================================================

CREATE TABLE carts (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         NUMBER NOT NULL,
    created_at      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- unit_price is captured at add-to-cart time for display, but checkout ALWAYS
-- recalculates from products/product_variants server-side — this column is
-- never trusted as the source of truth for a charge.
CREATE TABLE cart_items (
    id                      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cart_id                 NUMBER NOT NULL,
    product_variant_id      NUMBER NOT NULL,
    quantity                NUMBER NOT NULL,
    unit_price_snapshot      NUMBER(12,2) NOT NULL,
    added_at                TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_cart_items_cart_variant UNIQUE (cart_id, product_variant_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);

CREATE TABLE wishlists (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         NUMBER NOT NULL,
    created_at      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_wishlists_user UNIQUE (user_id),
    CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE wishlist_items (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    wishlist_id     NUMBER NOT NULL,
    product_id      NUMBER NOT NULL,
    added_at        TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_wishlist_items_wishlist_product UNIQUE (wishlist_id, product_id),
    CONSTRAINT fk_wishlist_items_wishlist FOREIGN KEY (wishlist_id) REFERENCES wishlists (id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_wishlist_items_wishlist ON wishlist_items (wishlist_id);
