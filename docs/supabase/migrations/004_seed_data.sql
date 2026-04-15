-- =====================================================
-- Seed Data for Food Ordering App
-- Version: 1.1
-- Date: 2026-04-14
-- Description: Cập nhật dữ liệu mẫu tiếng Việt chuyên nghiệp
-- =====================================================

-- =====================================================
-- Categories (Danh mục món ăn)
-- =====================================================
INSERT INTO categories (name, sort_order, is_active) VALUES 
    ('Cơm Trưa', 1, true),
    ('Bún & Phở', 2, true),
    ('Bánh Mì', 3, true),
    ('Ăn Vặt', 4, true),
    ('Trà Sữa & Giải Khát', 5, true),
    ('Tráng Miệng', 6, true)
ON CONFLICT DO NOTHING;

-- =====================================================
-- Sample Restaurants (Nhà hàng mẫu)
-- =====================================================
INSERT INTO restaurants (name, description, address, phone, image_url, rating, review_count, is_active, is_open, open_time, close_time, delivery_fee, min_order) VALUES 
    (
        'Cơm Tấm Sài Gòn 79',
        'Cơm tấm sườn bì chả đặc sản Sài Gòn, chuẩn vị truyền thống.',
        '123 Nguyễn Trãi, Quận 1, TP.HCM',
        '0901234567',
        'https://images.unsplash.com/photo-1555126634-323283e090fa?w=800',
        4.8, 1250, true, true, '07:00', '21:00', 15000, 30000
    ),
    (
        'Bánh Mì Huynh Đệ - Chợ Bến Thành',
        'Bánh mì đặc biệt đầy đủ topping, pate gan béo ngậy.',
        '456 Lê Lợi, Quận 1, TP.HCM',
        '0901234568',
        'https://images.unsplash.com/photo-1601050690597-df0568f70950?w=800',
        4.6, 850, true, true, '06:00', '22:00', 12000, 20000
    ),
    (
        'Phở Bò Gia Truyền',
        'Phở bò tái nạm chín, nước lèo trong vắt đậm đà từ xương ống.',
        '789 Pasteur, Quận 1, TP.HCM',
        '0901234569',
        'https://images.unsplash.com/photo-1582878826629-29b7a1a9d3f6?w=800',
        4.9, 2100, true, true, '06:00', '23:30', 20000, 50000
    ),
    (
        'Trà Sữa Miutea',
        'Trà sữa đậm vị trà, trân châu dai giòn sần sật.',
        '12 Đinh Tiên Hoàng, Quận 1, TP.HCM',
        '0901234570',
        'https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=800',
        4.5, 560, true, true, '09:00', '22:00', 10000, 25000
    )
ON CONFLICT DO NOTHING;

-- =====================================================
-- Sample Menu Items (Món ăn mẫu)
-- =====================================================
-- Cơm Tấm Sài Gòn 79
INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Cơm Tấm Sườn Bì Chả', 'Phần đặc biệt gồm sườn nướng, bì, chả và trứng ốp la.', 55000, true, true, 1
FROM restaurants r, categories c WHERE r.name = 'Cơm Tấm Sài Gòn 79' AND c.name = 'Cơm Trưa'
ON CONFLICT DO NOTHING;

INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Cơm Tấm Đùi Gà Nướng', 'Đùi gà góc tư nướng mật ong vàng óng.', 50000, true, false, 2
FROM restaurants r, categories c WHERE r.name = 'Cơm Tấm Sài Gòn 79' AND c.name = 'Cơm Trưa'
ON CONFLICT DO NOTHING;

-- Bánh Mì Huynh Đệ
INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Bánh Mì Thập Cẩm', 'Bánh mì giòn rụm với thịt nguội, pate, chả lụa và bơ.', 35000, true, true, 1
FROM restaurants r, categories c WHERE r.name = 'Bánh Mì Huynh Đệ - Chợ Bến Thành' AND c.name = 'Bánh Mì'
ON CONFLICT DO NOTHING;

-- Phở Bò Gia Truyền
INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Phở Tái Nạm', 'Bánh phở tươi, bò tái và nạm bò mềm tan.', 65000, true, true, 1
FROM restaurants r, categories c WHERE r.name = 'Phở Bò Gia Truyền' AND c.name = 'Bún & Phở'
ON CONFLICT DO NOTHING;

-- Trà Sữa Miutea
INSERT INTO menu_items (restaurant_id, category_id, name, description, price, is_available, is_featured, sort_order) 
SELECT r.id, c.id, 'Trà Sữa Truyền Thống', 'Trà sữa đen đậm vị kèm trân châu đen.', 35000, true, true, 1
FROM restaurants r, categories c WHERE r.name = 'Trà Sữa Miutea' AND c.name = 'Trà Sữa & Giải Khát'
ON CONFLICT DO NOTHING;

-- =====================================================
-- Sample Promotions (Khuyến mãi mẫu)
-- =====================================================
INSERT INTO promotions (code, name, description, discount_type, discount_value, min_order, max_discount, start_date, end_date, usage_limit, is_active) VALUES 
    ('BANMOI', 'Chào Bạn Mới', 'Giảm ngay 50% (tối đa 30k) cho đơn đầu tiên.', 'percent', 50, 40000, 30000, NOW(), NOW() + INTERVAL '30 days', 1000, true),
    ('FREESHIP', 'Miễn Phí Giao Hàng', 'Freeship tối đa 15k cho đơn hàng từ 100k.', 'fixed', 15000, 100000, NULL, NOW(), NOW() + INTERVAL '60 days', NULL, true),
    ('ANNGON', 'Ăn Ngon Cuối Tuần', 'Giảm 20k cho đơn từ 150k vào thứ 7 và CN.', 'fixed', 20000, 150000, NULL, NOW(), NOW() + INTERVAL '90 days', NULL, true)
ON CONFLICT DO NOTHING;

-- =====================================================
-- Sample Banners (Banner quảng cáo)
-- =====================================================
INSERT INTO banners (title, image_url, link_type, link_value, sort_order, is_active, start_date, end_date) VALUES 
    (
        'Ưu đãi bạn mới - Giảm ngay 50%',
        'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200',
        'promotion',
        'BANMOI',
        1,
        true,
        NOW(),
        NOW() + INTERVAL '30 days'
    ),
    (
        'Phở gia truyền - Chuẩn vị Hà Nội',
        'https://images.unsplash.com/photo-1552611052-33e04de081de?w=1200',
        'restaurant',
        (SELECT id FROM restaurants WHERE name = 'Phở Bò Gia Truyền'),
        2,
        true,
        NOW(),
        NULL
    ),
    (
        'Đặt đơn ngay - Freeship tận tay',
        'https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=1200',
        'promotion',
        'FREESHIP',
        3,
        true,
        NOW(),
        NOW() + INTERVAL '60 days'
    )
ON CONFLICT DO NOTHING;
