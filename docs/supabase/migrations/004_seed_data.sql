-- =====================================================
-- Seed Data for Food Ordering App
-- Version: 1.0
-- Date: 2026-04-04
-- Description: Initial seed data for development
-- =====================================================

-- =====================================================
-- Categories
-- =====================================================
INSERT INTO categories (name, sort_order, is_active) VALUES 
    ('Món chính', 1, true),
    ('Món phụ', 2, true),
    ('Đồ uống', 3, true),
    ('Tráng miệng', 4, true),
    ('Combo', 5, true)
ON CONFLICT DO NOTHING;

-- =====================================================
-- Sample Restaurants
-- =====================================================
INSERT INTO restaurants (name, description, address, phone, image_url, rating, review_count, is_active, is_open, open_time, close_time, delivery_fee, min_order) VALUES 
    (
        'Cơm Tấm 79',
        'Cơm tấm ngon, giá cả hợp lý',
        '123 Nguyễn Trãi, Quận 1, TP.HCM',
        '0901234567',
        'https://images.unsplash.com/photo-1555126634-323283e090fa?w=800',
        4.5, 120, true, true, '07:00', '22:00', 15000, 50000
    ),
    (
        'Bánh Mì Huynh Đệ',
        'Bánh mì pate thơm ngon',
        '456 Lê Lợi, Quận 1, TP.HCM',
        '0901234568',
        'https://images.unsplash.com/photo-1601050690597-df0568f70950?w=800',
        4.3, 85, true, true, '06:00', '21:00', 10000, 30000
    ),
    (
        'Phở Hàng Bè',
        'Phở bò tái nạm chín',
        '789 Pasteur, Quận 1, TP.HCM',
        '0901234569',
        'https://images.unsplash.com/photo-1582878826629-29b7a1a9d3f6?w=800',
        4.7, 200, true, true, '06:30', '23:00', 20000, 80000
    )
ON CONFLICT DO NOTHING;

-- =====================================================
-- Sample Menu Items
-- =====================================================
-- Restaurant 1: Cơm Tấm 79 (sẽ được tạo trước đó)
INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT 
    r.id,
    c.id,
    'Cơm tấm sường bì',
    'Cơm tấm với sường nướng và bì',
    45000,
    true,
    true,
    1
FROM restaurants r, categories c
WHERE r.name = 'Cơm Tấm 79' AND c.name = 'Món chính'
ON CONFLICT DO NOTHING;

INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Cơm tấm gà', 'Cơm tấm với gà nướng', 40000, true, false, 2
FROM restaurants r, categories c
WHERE r.name = 'Cơm Tấm 79' AND c.name = 'Món chính'
ON CONFLICT DO NOTHING;

INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Trứng chiên', 'Trứng chiên giòn', 10000, true, false, 1
FROM restaurants r, categories c
WHERE r.name = 'Cơm Tấm 79' AND c.name = 'Món phụ'
ON CONFLICT DO NOTHING;

INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Chè', 'Chè thái', 15000, true, false, 1
FROM restaurants r, categories c
WHERE r.name = 'Cơm Tấm 79' AND c.name = 'Tráng miệng'
ON CONFLICT DO NOTHING;

-- Restaurant 2: Bánh Mì Huynh Đệ
INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Bánh mì pate', 'Bánh mì pate trứng', 25000, true, true, 1
FROM restaurants r, categories c
WHERE r.name = 'Bánh Mì Huynh Đệ' AND c.name = 'Món chính'
ON CONFLICT DO NOTHING;

INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Bánh mì thịt', 'Bánh mì thịt nướng', 30000, true, false, 2
FROM restaurants r, categories c
WHERE r.name = 'Bánh Mì Huynh Đệ' AND c.name = 'Món chính'
ON CONFLICT DO NOTHING;

INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Sữa đá', 'Sữa tươi đá', 12000, true, false, 1
FROM restaurants r, categories c
WHERE r.name = 'Bánh Mì Huynh Đệ' AND c.name = 'Đồ uống'
ON CONFLICT DO NOTHING;

-- Restaurant 3: Phở Hàng Bè
INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Phở tái nạm', 'Phở bò tái nạm', 60000, true, true, 1
FROM restaurants r, categories c
WHERE r.name = 'Phở Hàng Bè' AND c.name = 'Món chính'
ON CONFLICT DO NOTHING;

INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Phở gân', 'Phở bò gân', 70000, true, false, 2
FROM restaurants r, categories c
WHERE r.name = 'Phở Hàng Bè' AND c.name = 'Món chính'
ON CONFLICT DO NOTHING;

INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Phở chín', 'Phở bò chín', 65000, true, false, 3
FROM restaurants r, categories c
WHERE r.name = 'Phở Hàng Bè' AND c.name = 'Món chính'
ON CONFLICT DO NOTHING;

INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Trà đá', 'Trà đá miễn phí', 0, true, false, 1
FROM restaurants r, categories c
WHERE r.name = 'Phở Hàng Bè' AND c.name = 'Đồ uống'
ON CONFLICT DO NOTHING;

-- =====================================================
-- Sample Promotions
-- =====================================================
INSERT INTO promotions (code, name, description, discount_type, discount_value, min_order, max_discount, start_date, end_date, usage_limit, is_active) VALUES 
    ('WELCOME50', 'Chào mừng khách mới', 'Giảm 50% cho đơn đầu tiên', 'percent', 50, 50000, 30000, NOW(), NOW() + INTERVAL '30 days', 1000, true),
    ('FREESHIP', 'Miễn phí vận chuyển', 'Miễn phí giao hàng cho đơn từ 100k', 'fixed', 15000, 100000, NULL, NOW(), NOW() + INTERVAL '60 days', NULL, true),
    ('GIAM20K', 'Giảm 20k', 'Giảm 20k cho mọi đơn', 'fixed', 20000, 150000, NULL, NOW(), NOW() + INTERVAL '90 days', NULL, true)
ON CONFLICT DO NOTHING;

-- =====================================================
-- Sample Banners
-- =====================================================
INSERT INTO banners (title, image_url, link_type, link_value, sort_order, is_active, start_date, end_date) VALUES 
    (
        'Chào mừng khách mới',
        'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200',
        'promotion',
        'WELCOME50',
        1,
        true,
        NOW(),
        NOW() + INTERVAL '30 days'
    ),
    (
        'Phở ngon giá hời',
        'https://images.unsplash.com/photo-1552611052-33e04de081de?w=1200',
        'restaurant',
        (SELECT id FROM restaurants WHERE name = 'Phở Hàng Bè'),
        2,
        true,
        NOW(),
        NULL
    ),
    (
        'Miễn phí vận chuyển',
        'https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=1200',
        'promotion',
        'FREESHIP',
        3,
        true,
        NOW(),
        NOW() + INTERVAL '60 days'
    )
ON CONFLICT DO NOTHING;

-- =====================================================
-- Notes for deployment:
-- 1. After creating Supabase project, run migrations in order:
--    - 001_initial_schema.sql
--    - 002_rpc_functions.sql  
--    - 003_rls_policies.sql
--    - 004_seed_data.sql
--
-- 2. Create auth users first, then update users table with auth_id
--    Example:
--    INSERT INTO auth.users (email, password) VALUES ('admin@foodapp.com', 'password123');
--    UPDATE users SET auth_id = (SELECT id FROM auth.users WHERE email = 'admin@foodapp.com')
--    WHERE email = 'admin@foodapp.com';
--
-- 3. For testing, create users with role 'admin' for admin app access
--
-- 4. Storage buckets should be created manually in Supabase dashboard:
--    - avatars (for user avatars)
--    - restaurants (for restaurant images)
--    - menu-items (for food images)
--    - banners (for banner images)
