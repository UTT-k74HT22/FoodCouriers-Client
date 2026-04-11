-- =====================================================
-- RLS Policies for Food Ordering App
-- Version: 1.0
-- Date: 2026-04-04
-- Description: Row Level Security policies
-- =====================================================

-- =====================================================
-- TABLE: users
-- =====================================================
-- Customers can read/update their own profile
CREATE POLICY "users_select_own" ON users
    FOR SELECT USING (auth_id = auth.uid());

CREATE POLICY "users_select_admin" ON users
    FOR SELECT USING (
        EXISTS (SELECT 1 FROM users u WHERE u.auth_id = auth.uid() AND u.role = 'admin')
    );

CREATE POLICY "users_insert_own" ON users
    FOR INSERT WITH CHECK (auth_id = auth.uid());

CREATE POLICY "users_update_own" ON users
    FOR UPDATE USING (auth_id = auth.uid());

CREATE POLICY "users_update_admin" ON users
    FOR UPDATE USING (
        EXISTS (SELECT 1 FROM users u WHERE u.auth_id = auth.uid() AND u.role = 'admin')
    )
    WITH CHECK (
        EXISTS (SELECT 1 FROM users u WHERE u.auth_id = auth.uid() AND u.role = 'admin')
    );

-- =====================================================
-- TABLE: user_addresses
-- =====================================================
CREATE POLICY "user_addresses_select" ON user_addresses
    FOR SELECT USING (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
    );

CREATE POLICY "user_addresses_insert" ON user_addresses
    FOR INSERT WITH CHECK (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
    );

CREATE POLICY "user_addresses_update_own" ON user_addresses
    FOR UPDATE USING (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
    );

CREATE POLICY "user_addresses_delete_own" ON user_addresses
    FOR DELETE USING (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
    );

-- =====================================================
-- TABLE: restaurants
-- =====================================================
-- Everyone can read active restaurants
CREATE POLICY "restaurants_select_all" ON restaurants
    FOR SELECT USING (is_active = true);

-- Only admins can insert/update/delete
CREATE POLICY "restaurants_manage" ON restaurants
    FOR ALL USING (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    )
    WITH CHECK (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

-- =====================================================
-- TABLE: categories
-- =====================================================
CREATE POLICY "categories_select" ON categories
    FOR SELECT USING (is_active = true);

CREATE POLICY "categories_insert_admin" ON categories
    FOR INSERT
    WITH CHECK (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

CREATE POLICY "categories_update_admin" ON categories
    FOR UPDATE
    USING (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    )
    WITH CHECK (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

CREATE POLICY "categories_delete_admin" ON categories
    FOR DELETE
    USING (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

-- =====================================================
-- TABLE: restaurant_staff
-- =====================================================
CREATE POLICY "restaurant_staff_select" ON restaurant_staff
    FOR SELECT USING (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
        OR EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

CREATE POLICY "restaurant_staff_manage" ON restaurant_staff
    FOR ALL USING (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    )
    WITH CHECK (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

-- =====================================================
-- TABLE: menu_items
-- =====================================================
-- Read available items from active restaurants
CREATE POLICY "menu_items_select" ON menu_items
    FOR SELECT USING (
        is_available = true AND
        restaurant_id IN (SELECT id FROM restaurants WHERE is_active = true)
    );

CREATE POLICY "menu_items_manage" ON menu_items
    FOR ALL USING (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role IN ('admin', 'staff'))
    )
    WITH CHECK (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role IN ('admin', 'staff'))
    );

-- =====================================================
-- TABLE: orders (Customer)
-- =====================================================
-- Customers can see their own orders
CREATE POLICY "orders_select_own" ON orders
    FOR SELECT USING (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
    );

-- Customers can create orders
CREATE POLICY "orders_insert" ON orders
    FOR INSERT WITH CHECK (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
    );

-- =====================================================
-- TABLE: orders (Admin/Staff)
-- =====================================================
-- Admins can see all orders
CREATE POLICY "orders_select_all" ON orders
    FOR SELECT USING (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

-- Admins can update orders
CREATE POLICY "orders_update_all" ON orders
    FOR UPDATE USING (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

-- =====================================================
-- TABLE: order_items
-- =====================================================
CREATE POLICY "order_items_select_own" ON order_items
    FOR SELECT USING (
        order_id IN (SELECT id FROM orders WHERE user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()))
        OR EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

-- =====================================================
-- TABLE: order_status_logs
-- =====================================================
CREATE POLICY "order_status_logs_select" ON order_status_logs
    FOR SELECT USING (
        order_id IN (SELECT id FROM orders WHERE user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()))
        OR EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

-- =====================================================
-- TABLE: promotions
-- =====================================================
CREATE POLICY "promotions_select" ON promotions
    FOR SELECT USING (is_active = true);

CREATE POLICY "promotions_manage" ON promotions
    FOR ALL USING (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    )
    WITH CHECK (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

-- =====================================================
-- TABLE: promotion_usages
-- =====================================================
CREATE POLICY "promotion_usages_select" ON promotion_usages
    FOR SELECT USING (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
        OR EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

-- =====================================================
-- TABLE: reviews
-- =====================================================
-- Customers can see all reviews
CREATE POLICY "reviews_select" ON reviews
    FOR SELECT USING (is_visible = true);

-- Customers can insert their own reviews
CREATE POLICY "reviews_insert" ON reviews
    FOR INSERT WITH CHECK (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
    );

-- Admins can manage reviews
CREATE POLICY "reviews_manage" ON reviews
    FOR ALL USING (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    )
    WITH CHECK (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

-- =====================================================
-- TABLE: banners
-- =====================================================
CREATE POLICY "banners_select" ON banners
    FOR SELECT USING (
        is_active = true AND
        (start_date IS NULL OR start_date <= NOW()) AND
        (end_date IS NULL OR end_date >= NOW())
    );

CREATE POLICY "banners_manage" ON banners
    FOR ALL USING (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    )
    WITH CHECK (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

-- =====================================================
-- TABLE: notifications
-- =====================================================
CREATE POLICY "notifications_select_own" ON notifications
    FOR SELECT USING (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
    );

CREATE POLICY "notifications_update_own" ON notifications
    FOR UPDATE USING (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
    );

-- =====================================================
-- TABLE: admin_logs
-- =====================================================
-- Only admins can read admin logs
CREATE POLICY "admin_logs_select" ON admin_logs
    FOR SELECT USING (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

CREATE POLICY "admin_logs_insert" ON admin_logs
    FOR INSERT WITH CHECK (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

-- =====================================================
-- TABLE: carts (for future multi-device sync)
-- =====================================================
CREATE POLICY "carts_select_own" ON carts
    FOR SELECT USING (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
    );

CREATE POLICY "carts_manage" ON carts
    FOR ALL USING (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
    )
    WITH CHECK (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
    );

-- =====================================================
-- TABLE: cart_items
-- =====================================================
CREATE POLICY "cart_items_manage" ON cart_items
    FOR ALL USING (
        cart_id IN (SELECT id FROM carts WHERE user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()))
    )
    WITH CHECK (
        cart_id IN (SELECT id FROM carts WHERE user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()))
    );

-- =====================================================
-- TABLE: payment_transactions
-- =====================================================
CREATE POLICY "payment_transactions_select" ON payment_transactions
    FOR SELECT USING (
        order_id IN (SELECT id FROM orders WHERE user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()))
        OR EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );
