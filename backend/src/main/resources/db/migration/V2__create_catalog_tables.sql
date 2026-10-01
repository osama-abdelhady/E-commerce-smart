-- ============================================================
-- V2: Catalog tables
-- CATEGORIES, BRANDS, PRODUCTS, PRODUCT_VARIANTS, PRODUCT_IMAGES,
-- INVENTORY, INVENTORY_MOVEMENTS
-- ============================================================

CREATE TABLE categories (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_id       NUMBER,
    slug            VARCHAR2(150) NOT NULL,
    name            VARCHAR2(150) NOT NULL,
    description     VARCHAR2(1000),
    image_url       VARCHAR2(500),
    is_active       NUMBER(1) DEFAULT 1 NOT NULL,
    display_order   NUMBER DEFAULT 0 NOT NULL,
    created_at      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_categories_slug UNIQUE (slug),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id),
    CONSTRAINT ck_categories_is_active CHECK (is_active IN (0,1))
);

CREATE TABLE brands (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug            VARCHAR2(150) NOT NULL,
    name            VARCHAR2(150) NOT NULL,
    logo_url        VARCHAR2(500),
    is_active       NUMBER(1) DEFAULT 1 NOT NULL,
    CONSTRAINT uq_brands_slug UNIQUE (slug),
    CONSTRAINT ck_brands_is_active CHECK (is_active IN (0,1))
);

CREATE TABLE products (
    id                  NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku                 VARCHAR2(60) NOT NULL,
    slug                VARCHAR2(200) NOT NULL,
    name                VARCHAR2(255) NOT NULL,
    description         CLOB,
    price               NUMBER(12,2) NOT NULL,
    discount_price      NUMBER(12,2),
    currency            VARCHAR2(3) DEFAULT 'USD' NOT NULL,
    category_id         NUMBER NOT NULL,
    brand_id            NUMBER,
    status              VARCHAR2(20) DEFAULT 'DRAFT' NOT NULL,
    is_best_seller      NUMBER(1) DEFAULT 0 NOT NULL,
    is_new_arrival      NUMBER(1) DEFAULT 0 NOT NULL,
    average_rating      NUMBER(3,2) DEFAULT 0 NOT NULL,
    review_count        NUMBER DEFAULT 0 NOT NULL,
    version             NUMBER DEFAULT 0 NOT NULL,
    created_at          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT uq_products_slug UNIQUE (slug),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    CONSTRAINT ck_products_status CHECK (status IN ('DRAFT','ACTIVE','INACTIVE','ARCHIVED')),
    CONSTRAINT ck_products_price CHECK (price >= 0),
    CONSTRAINT ck_products_discount_price CHECK (discount_price IS NULL OR discount_price >= 0),
    CONSTRAINT ck_products_rating CHECK (average_rating BETWEEN 0 AND 5)
);

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_brand ON products (brand_id);
CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_price ON products (price);

CREATE TABLE product_images (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id      NUMBER NOT NULL,
    url             VARCHAR2(500) NOT NULL,
    alt_text        VARCHAR2(255),
    display_order   NUMBER DEFAULT 0 NOT NULL,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_images_product ON product_images (product_id);

CREATE TABLE product_variants (
    id                  NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id          NUMBER NOT NULL,
    sku                 VARCHAR2(60) NOT NULL,
    size                VARCHAR2(20),
    color               VARCHAR2(50),
    color_hex           VARCHAR2(7),
    price_override      NUMBER(12,2),
    is_active           NUMBER(1) DEFAULT 1 NOT NULL,
    version             NUMBER DEFAULT 0 NOT NULL,
    CONSTRAINT uq_product_variants_sku UNIQUE (sku),
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT ck_product_variants_price CHECK (price_override IS NULL OR price_override >= 0)
);

CREATE INDEX idx_product_variants_product ON product_variants (product_id);

-- One inventory row per sellable variant. quantity_available is what's shown
-- to shoppers; quantity_reserved is stock held by open carts/pending orders
-- so two concurrent checkouts can't both claim the last unit.
CREATE TABLE inventory (
    id                      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_variant_id     NUMBER NOT NULL,
    quantity_available      NUMBER DEFAULT 0 NOT NULL,
    quantity_reserved       NUMBER DEFAULT 0 NOT NULL,
    low_stock_threshold     NUMBER DEFAULT 5 NOT NULL,
    version                 NUMBER DEFAULT 0 NOT NULL,
    updated_at               TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_inventory_variant UNIQUE (product_variant_id),
    CONSTRAINT fk_inventory_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id) ON DELETE CASCADE,
    CONSTRAINT ck_inventory_available CHECK (quantity_available >= 0),
    CONSTRAINT ck_inventory_reserved CHECK (quantity_reserved >= 0)
);

CREATE TABLE inventory_movements (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inventory_id    NUMBER NOT NULL,
    movement_type   VARCHAR2(20) NOT NULL,
    quantity_delta  NUMBER NOT NULL,
    reason          VARCHAR2(255),
    reference_type  VARCHAR2(30),
    reference_id    NUMBER,
    created_by      NUMBER,
    created_at      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_inv_movements_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (id) ON DELETE CASCADE,
    CONSTRAINT ck_inv_movements_type CHECK (movement_type IN
        ('RESTOCK','RESERVATION','RESERVATION_RELEASE','SALE','RETURN','ADJUSTMENT'))
);

CREATE INDEX idx_inv_movements_inventory ON inventory_movements (inventory_id);
CREATE INDEX idx_inv_movements_reference ON inventory_movements (reference_type, reference_id);
