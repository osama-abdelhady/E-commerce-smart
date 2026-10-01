-- ============================================================
-- V6: Development seed data
-- Passwords below are BCrypt hashes of "Password123!" — dev/test only.
-- ============================================================

INSERT INTO roles (name) VALUES ('CUSTOMER');
INSERT INTO roles (name) VALUES ('ADMIN');

INSERT INTO users (email, password_hash, full_name, status, email_verified_at)
VALUES ('admin@tailoredplatform.dev', '$2b$10$k4IuAfK43f8uhz41YvVwT.lDpyGCgZRkTbMUyU9DDdXuM2Y/nt7Va',
        'Platform Admin', 'ACTIVE', SYSTIMESTAMP);

INSERT INTO users (email, password_hash, full_name, status, email_verified_at)
VALUES ('customer@tailoredplatform.dev', '$2b$10$k4IuAfK43f8uhz41YvVwT.lDpyGCgZRkTbMUyU9DDdXuM2Y/nt7Va',
        'Sample Customer', 'ACTIVE', SYSTIMESTAMP);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'admin@tailoredplatform.dev' AND r.name = 'ADMIN';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'admin@tailoredplatform.dev' AND r.name = 'CUSTOMER';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'customer@tailoredplatform.dev' AND r.name = 'CUSTOMER';

INSERT INTO addresses (user_id, label, full_name, line1, city, state, postal_code, country, phone, is_default)
SELECT id, 'Home', 'Sample Customer', '221B Baker Street', 'Cairo', 'Cairo Governorate', '11511', 'EG', '+201000000000', 1
FROM users WHERE email = 'customer@tailoredplatform.dev';

-- Categories
INSERT INTO categories (slug, name, description, is_active, display_order) VALUES ('business', 'Business Suits', 'Sharp tailoring for the office and boardroom.', 1, 1);
INSERT INTO categories (slug, name, description, is_active, display_order) VALUES ('wedding', 'Wedding Suits', 'Formal suits for grooms and groomsmen.', 1, 2);
INSERT INTO categories (slug, name, description, is_active, display_order) VALUES ('casual', 'Casual Suits', 'Relaxed separates for smart-casual occasions.', 1, 3);
INSERT INTO categories (slug, name, description, is_active, display_order) VALUES ('accessories', 'Accessories', 'Ties, belts, and finishing touches.', 1, 4);

-- Brands
INSERT INTO brands (slug, name, is_active) VALUES ('hugo-boss', 'Hugo Boss', 1);
INSERT INTO brands (slug, name, is_active) VALUES ('armani', 'Armani', 1);
INSERT INTO brands (slug, name, is_active) VALUES ('suitsupply', 'Suitsupply', 1);
INSERT INTO brands (slug, name, is_active) VALUES ('canali', 'Canali', 1);

-- Products
INSERT INTO products (sku, slug, name, description, price, discount_price, category_id, brand_id, status, is_best_seller, is_new_arrival, average_rating, review_count)
SELECT 'SUIT-BUS-001', 'charcoal-wool-business-suit', 'Charcoal Wool Business Suit',
       'A precision-cut two-piece suit in fine Italian wool, finished with horn buttons and a half-canvas construction.',
       549.00, 449.00, c.id, b.id, 'ACTIVE', 1, 0, 4.6, 128
FROM categories c, brands b WHERE c.slug = 'business' AND b.slug = 'hugo-boss';

INSERT INTO products (sku, slug, name, description, price, category_id, brand_id, status, is_best_seller, is_new_arrival, average_rating, review_count)
SELECT 'SUIT-WED-001', 'ivory-wedding-tuxedo', 'Ivory Wedding Tuxedo',
       'A statement tuxedo in ivory wool-silk blend, tailored for the modern groom.',
       699.00, c.id, b.id, 'ACTIVE', 0, 1, 4.8, 42
FROM categories c, brands b WHERE c.slug = 'wedding' AND b.slug = 'armani';

INSERT INTO products (sku, slug, name, description, price, category_id, brand_id, status, is_best_seller, is_new_arrival, average_rating, review_count)
SELECT 'SUIT-CAS-001', 'sand-linen-casual-suit', 'Sand Linen Casual Suit',
       'A breathable linen-blend suit built for warm-weather smart-casual dressing.',
       379.00, c.id, b.id, 'ACTIVE', 1, 1, 4.4, 76
FROM categories c, brands b WHERE c.slug = 'casual' AND b.slug = 'suitsupply';

INSERT INTO products (sku, slug, name, description, price, category_id, brand_id, status, is_best_seller, is_new_arrival, average_rating, review_count)
SELECT 'SUIT-BUS-002', 'navy-pinstripe-suit', 'Navy Pinstripe Suit',
       'A classic pinstripe cut in mid-weight wool, suited to year-round boardroom wear.',
       599.00, c.id, b.id, 'ACTIVE', 1, 0, 4.7, 95
FROM categories c, brands b WHERE c.slug = 'business' AND b.slug = 'canali';

-- Images
INSERT INTO product_images (product_id, url, alt_text, display_order)
SELECT id, 'https://images.tailoredplatform.dev/products/charcoal-wool-business-suit-1.jpg', 'Charcoal Wool Business Suit, front view', 1
FROM products WHERE sku = 'SUIT-BUS-001';
INSERT INTO product_images (product_id, url, alt_text, display_order)
SELECT id, 'https://images.tailoredplatform.dev/products/charcoal-wool-business-suit-2.jpg', 'Charcoal Wool Business Suit, detail view', 2
FROM products WHERE sku = 'SUIT-BUS-001';
INSERT INTO product_images (product_id, url, alt_text, display_order)
SELECT id, 'https://images.tailoredplatform.dev/products/ivory-wedding-tuxedo-1.jpg', 'Ivory Wedding Tuxedo, front view', 1
FROM products WHERE sku = 'SUIT-WED-001';
INSERT INTO product_images (product_id, url, alt_text, display_order)
SELECT id, 'https://images.tailoredplatform.dev/products/sand-linen-casual-suit-1.jpg', 'Sand Linen Casual Suit, front view', 1
FROM products WHERE sku = 'SUIT-CAS-001';
INSERT INTO product_images (product_id, url, alt_text, display_order)
SELECT id, 'https://images.tailoredplatform.dev/products/navy-pinstripe-suit-1.jpg', 'Navy Pinstripe Suit, front view', 1
FROM products WHERE sku = 'SUIT-BUS-002';

-- Variants (size x color) + inventory for the charcoal business suit
INSERT INTO product_variants (product_id, sku, size, color, color_hex, is_active)
SELECT id, 'SUIT-BUS-001-40R-CHR', '40R', 'Charcoal', '#36454F', 1 FROM products WHERE sku = 'SUIT-BUS-001';
INSERT INTO product_variants (product_id, sku, size, color, color_hex, is_active)
SELECT id, 'SUIT-BUS-001-42R-CHR', '42R', 'Charcoal', '#36454F', 1 FROM products WHERE sku = 'SUIT-BUS-001';
INSERT INTO product_variants (product_id, sku, size, color, color_hex, is_active)
SELECT id, 'SUIT-BUS-001-44R-CHR', '44R', 'Charcoal', '#36454F', 1 FROM products WHERE sku = 'SUIT-BUS-001';

INSERT INTO product_variants (product_id, sku, size, color, color_hex, is_active)
SELECT id, 'SUIT-WED-001-40R-IVR', '40R', 'Ivory', '#FFFFF0', 1 FROM products WHERE sku = 'SUIT-WED-001';
INSERT INTO product_variants (product_id, sku, size, color, color_hex, is_active)
SELECT id, 'SUIT-WED-001-42R-IVR', '42R', 'Ivory', '#FFFFF0', 1 FROM products WHERE sku = 'SUIT-WED-001';

INSERT INTO product_variants (product_id, sku, size, color, color_hex, is_active)
SELECT id, 'SUIT-CAS-001-40R-SND', '40R', 'Sand', '#C2B280', 1 FROM products WHERE sku = 'SUIT-CAS-001';
INSERT INTO product_variants (product_id, sku, size, color, color_hex, is_active)
SELECT id, 'SUIT-CAS-001-42R-SND', '42R', 'Sand', '#C2B280', 1 FROM products WHERE sku = 'SUIT-CAS-001';

INSERT INTO product_variants (product_id, sku, size, color, color_hex, is_active)
SELECT id, 'SUIT-BUS-002-40R-NVY', '40R', 'Navy', '#1B2A4A', 1 FROM products WHERE sku = 'SUIT-BUS-002';
INSERT INTO product_variants (product_id, sku, size, color, color_hex, is_active)
SELECT id, 'SUIT-BUS-002-42R-NVY', '42R', 'Navy', '#1B2A4A', 1 FROM products WHERE sku = 'SUIT-BUS-002';

INSERT INTO inventory (product_variant_id, quantity_available, low_stock_threshold)
SELECT id, 25, 5 FROM product_variants WHERE sku = 'SUIT-BUS-001-40R-CHR';
INSERT INTO inventory (product_variant_id, quantity_available, low_stock_threshold)
SELECT id, 3, 5 FROM product_variants WHERE sku = 'SUIT-BUS-001-42R-CHR';
INSERT INTO inventory (product_variant_id, quantity_available, low_stock_threshold)
SELECT id, 18, 5 FROM product_variants WHERE sku = 'SUIT-BUS-001-44R-CHR';
INSERT INTO inventory (product_variant_id, quantity_available, low_stock_threshold)
SELECT id, 10, 3 FROM product_variants WHERE sku = 'SUIT-WED-001-40R-IVR';
INSERT INTO inventory (product_variant_id, quantity_available, low_stock_threshold)
SELECT id, 8, 3 FROM product_variants WHERE sku = 'SUIT-WED-001-42R-IVR';
INSERT INTO inventory (product_variant_id, quantity_available, low_stock_threshold)
SELECT id, 30, 5 FROM product_variants WHERE sku = 'SUIT-CAS-001-40R-SND';
INSERT INTO inventory (product_variant_id, quantity_available, low_stock_threshold)
SELECT id, 22, 5 FROM product_variants WHERE sku = 'SUIT-CAS-001-42R-SND';
INSERT INTO inventory (product_variant_id, quantity_available, low_stock_threshold)
SELECT id, 15, 5 FROM product_variants WHERE sku = 'SUIT-BUS-002-40R-NVY';
INSERT INTO inventory (product_variant_id, quantity_available, low_stock_threshold)
SELECT id, 12, 5 FROM product_variants WHERE sku = 'SUIT-BUS-002-42R-NVY';

-- Coupon
INSERT INTO coupons (code, discount_type, discount_value, minimum_order_amount, max_redemptions, max_redemptions_per_user, valid_from, valid_until, is_active)
VALUES ('WELCOME10', 'PERCENTAGE', 10, 100, 500, 1, SYSTIMESTAMP - 1, SYSTIMESTAMP + 365, 1);
