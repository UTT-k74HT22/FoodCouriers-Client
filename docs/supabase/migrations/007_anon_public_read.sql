-- =====================================================
-- Allow anonymous (public) read access to public data
-- Fixes: permission denied for table restaurants
-- =====================================================

-- =====================================================
-- TABLE: restaurants
-- =====================================================
ALTER TABLE IF EXISTS public.restaurants ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "restaurants_select" ON restaurants;
DROP POLICY IF EXISTS "restaurants_public_read" ON restaurants;

CREATE POLICY "restaurants_public_read" ON restaurants
FOR SELECT
TO anon, authenticated
USING (is_active = true);

GRANT SELECT ON restaurants TO anon, authenticated;

-- =====================================================
-- TABLE: categories
-- =====================================================
ALTER TABLE IF EXISTS public.categories ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "categories_select" ON categories;
DROP POLICY IF EXISTS "categories_public_read" ON categories;

CREATE POLICY "categories_public_read" ON categories
FOR SELECT
TO anon, authenticated
USING (is_active = true);

GRANT SELECT ON categories TO anon, authenticated;

-- =====================================================
-- TABLE: menu_items
-- =====================================================
ALTER TABLE IF EXISTS public.menu_items ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "menu_items_select" ON menu_items;
DROP POLICY IF EXISTS "menu_items_public_read" ON menu_items;

CREATE POLICY "menu_items_public_read" ON menu_items
FOR SELECT
TO anon, authenticated
USING (is_available = true);

GRANT SELECT ON menu_items TO anon, authenticated;

-- =====================================================
-- TABLE: banners
-- =====================================================
ALTER TABLE IF EXISTS public.banners ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "banners_select" ON banners;
DROP POLICY IF EXISTS "banners_public_read" ON banners;

CREATE POLICY "banners_public_read" ON banners
FOR SELECT
TO anon, authenticated
USING (is_active = true);

GRANT SELECT ON banners TO anon, authenticated;

-- =====================================================
-- TABLE: promotions
-- =====================================================
ALTER TABLE IF EXISTS public.promotions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "promotions_select" ON promotions;
DROP POLICY IF EXISTS "promotions_public_read" ON promotions;

CREATE POLICY "promotions_public_read" ON promotions
FOR SELECT
TO anon, authenticated
USING (is_active = true);

GRANT SELECT ON promotions TO anon, authenticated;

-- =====================================================
-- TABLE: reviews
-- =====================================================
ALTER TABLE IF EXISTS public.reviews ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "reviews_select" ON reviews;
DROP POLICY IF EXISTS "reviews_public_read" ON reviews;

CREATE POLICY "reviews_public_read" ON reviews
FOR SELECT
TO anon, authenticated
USING (is_visible = true);

GRANT SELECT ON reviews TO anon, authenticated;