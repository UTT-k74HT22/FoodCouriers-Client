-- =====================================================
-- Fix RLS Policies - Admin Full, Client Custom
-- Run this to reset and recreate policies
-- =====================================================

-- 1. Delete ALL existing policies
DO $$ 
DECLARE 
    r RECORD;
BEGIN
    FOR r IN (
        SELECT schemaname, tablename, policyname
        FROM pg_policies
        WHERE schemaname = 'public'
    )
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON %I.%I;', r.policyname, r.schemaname, r.tablename);
    END LOOP;
END $$;

-- 2. Create helper function for admin check
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.users 
        WHERE auth_id = auth.uid() AND role = 'admin'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. Create helper function for staff check
CREATE OR REPLACE FUNCTION public.is_staff()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.users 
        WHERE auth_id = auth.uid() AND role IN ('admin', 'staff')
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- TABLE: users
-- =====================================================
-- Client: read/update own profile
CREATE POLICY "users_select_own" ON users FOR SELECT USING (auth_id = auth.uid());
CREATE POLICY "users_update_own" ON users FOR UPDATE USING (auth_id = auth.uid());
-- Admin: full access
CREATE POLICY "users_admin_all" ON users FOR ALL USING (is_admin()) WITH CHECK (is_admin());

-- =====================================================
-- TABLE: user_addresses
-- =====================================================
CREATE POLICY "user_addresses_all" ON user_addresses FOR ALL USING (user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())) WITH CHECK (user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()));

-- =====================================================
-- TABLE: restaurants
-- =====================================================
-- Client: read active restaurants
CREATE POLICY "restaurants_select" ON restaurants FOR SELECT USING (is_active = true);
-- Admin: full access
CREATE POLICY "restaurants_admin_all" ON restaurants FOR ALL USING (is_admin()) WITH CHECK (is_admin());

-- =====================================================
-- TABLE: categories
-- =====================================================
CREATE POLICY "categories_select" ON categories FOR SELECT USING (is_active = true);
CREATE POLICY "categories_admin_all" ON categories FOR ALL USING (is_admin()) WITH CHECK (is_admin());

-- =====================================================
-- TABLE: menu_items
-- =====================================================
-- Client: read available menu items
CREATE POLICY "menu_items_select" ON menu_items FOR SELECT USING (is_available = true);
-- Admin/Staff: full access
CREATE POLICY "menu_items_staff_all" ON menu_items FOR ALL USING (is_staff()) WITH CHECK (is_staff());

-- =====================================================
-- TABLE: orders
-- =====================================================
-- Client: read own orders, create new
CREATE POLICY "orders_select_own" ON orders FOR SELECT USING (user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()));
CREATE POLICY "orders_insert" ON orders FOR INSERT WITH CHECK (user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()));
-- Admin: full access
CREATE POLICY "orders_admin_all" ON orders FOR ALL USING (is_admin()) WITH CHECK (is_admin());

-- =====================================================
-- TABLE: order_items
-- =====================================================
CREATE POLICY "order_items_select_own" ON order_items FOR SELECT USING (order_id IN (SELECT id FROM orders WHERE user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())));

-- =====================================================
-- TABLE: order_status_logs
-- =====================================================
CREATE POLICY "order_status_logs_select" ON order_status_logs FOR SELECT USING (order_id IN (SELECT id FROM orders WHERE user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())));

-- =====================================================
-- TABLE: promotions
-- =====================================================
CREATE POLICY "promotions_select" ON promotions FOR SELECT USING (is_active = true);
CREATE POLICY "promotions_admin_all" ON promotions FOR ALL USING (is_admin()) WITH CHECK (is_admin());

-- =====================================================
-- TABLE: reviews
-- =====================================================
CREATE POLICY "reviews_select" ON reviews FOR SELECT USING (is_visible = true);
CREATE POLICY "reviews_insert" ON reviews FOR INSERT WITH CHECK (user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()));
CREATE POLICY "reviews_admin_all" ON reviews FOR ALL USING (is_admin()) WITH CHECK (is_admin());

-- =====================================================
-- TABLE: banners
-- =====================================================
CREATE POLICY "banners_select" ON banners FOR SELECT USING (is_active = true);
CREATE POLICY "banners_admin_all" ON banners FOR ALL USING (is_admin()) WITH CHECK (is_admin());

-- =====================================================
-- TABLE: notifications
-- =====================================================
CREATE POLICY "notifications_select" ON notifications FOR SELECT USING (user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()));
CREATE POLICY "notifications_update" ON notifications FOR UPDATE USING (user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()));

-- =====================================================
-- TABLE: carts
-- =====================================================
CREATE POLICY "carts_all" ON carts FOR ALL USING (user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())) WITH CHECK (user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()));

-- =====================================================
-- TABLE: cart_items
-- =====================================================
CREATE POLICY "cart_items_all" ON cart_items FOR ALL USING (cart_id IN (SELECT id FROM carts WHERE user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()))) WITH CHECK (cart_id IN (SELECT id FROM carts WHERE user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())));

-- =====================================================
-- TABLE: restaurant_staff
-- =====================================================
CREATE POLICY "restaurant_staff_select" ON restaurant_staff FOR SELECT USING (user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()));
CREATE POLICY "restaurant_staff_admin" ON restaurant_staff FOR ALL USING (is_admin()) WITH CHECK (is_admin());

-- =====================================================
-- TABLE: promotion_usages
-- =====================================================
CREATE POLICY "promotion_usages_select" ON promotion_usages FOR SELECT USING (user_id IN (SELECT id FROM users WHERE auth_id = auth.uid()));

-- =====================================================
-- TABLE: admin_logs
-- =====================================================
CREATE POLICY "admin_logs_admin" ON admin_logs FOR ALL USING (is_admin()) WITH CHECK (is_admin());

-- =====================================================
-- TABLE: payment_transactions
-- =====================================================
CREATE POLICY "payment_transactions_select" ON payment_transactions FOR SELECT USING (order_id IN (SELECT id FROM orders WHERE user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())));
