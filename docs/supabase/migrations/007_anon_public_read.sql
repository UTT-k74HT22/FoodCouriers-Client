-- =====================================================
-- Migration: 007_anon_public_read.sql
-- Description: Bản vá cuối cùng cho lỗi 403 Forbidden
-- Cập nhật: 2026-04-14 - Fix UPSERT & RLS Recursion
-- =====================================================

-- 1. Đảm bảo cấu trúc bảng hỗ trợ UPSERT
ALTER TABLE public.cart_items DROP CONSTRAINT IF EXISTS cart_items_cart_id_menu_item_id_key;
ALTER TABLE public.cart_items ADD CONSTRAINT cart_items_cart_id_menu_item_id_key UNIQUE (cart_id, menu_item_id);

-- 2. Thêm cột auth_id trực tiếp vào carts để tối ưu RLS
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='carts' AND column_name='user_auth_id') THEN
        ALTER TABLE public.carts ADD COLUMN user_auth_id UUID REFERENCES auth.users(id);
    END IF;
END $$;

-- 3. Hàm tự động tạo profile và giỏ hàng (Đã fix đồng bộ auth_id)
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
DECLARE
    new_user_uuid UUID;
BEGIN
    INSERT INTO public.users (auth_id, email, full_name, avatar_url, role)
    VALUES (
        new.id,
        new.email,
        COALESCE(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name', 'Người dùng mới'),
        new.raw_user_meta_data->>'avatar_url',
        'customer'
    )
    ON CONFLICT (auth_id) DO UPDATE SET
        email = EXCLUDED.email,
        full_name = EXCLUDED.full_name,
        avatar_url = EXCLUDED.avatar_url
    RETURNING id INTO new_user_uuid;

    INSERT INTO public.carts (user_id, user_auth_id)
    VALUES (new_user_uuid, new.id)
    ON CONFLICT (user_id) DO UPDATE SET user_auth_id = EXCLUDED.user_auth_id;

    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Re-set Trigger
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created AFTER INSERT ON auth.users FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 4. RLS siêu đơn giản và hiệu quả
ALTER TABLE public.carts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cart_items ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "carts_manage_own" ON public.carts;
CREATE POLICY "carts_manage_own" ON public.carts FOR ALL TO authenticated 
USING (user_auth_id = auth.uid()) WITH CHECK (user_auth_id = auth.uid());

DROP POLICY IF EXISTS "cart_items_manage_own" ON public.cart_items;
CREATE POLICY "cart_items_manage_own" ON public.cart_items FOR ALL TO authenticated 
USING (cart_id IN (SELECT id FROM public.carts WHERE user_auth_id = auth.uid()))
WITH CHECK (cart_id IN (SELECT id FROM public.carts WHERE user_auth_id = auth.uid()));

-- 5. Cấp quyền truy cập công khai (SELECT)
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT SELECT ON public.restaurants TO anon, authenticated;
GRANT SELECT ON public.categories TO anon, authenticated;
GRANT SELECT ON public.menu_items TO anon, authenticated;
GRANT SELECT ON public.promotions TO anon, authenticated;
GRANT SELECT ON public.banners TO anon, authenticated;
GRANT SELECT ON public.reviews TO anon, authenticated;
GRANT SELECT (id, full_name, avatar_url) ON public.users TO anon, authenticated;

-- 6. Quyền thao tác dữ liệu cho Authenticated
GRANT ALL ON public.carts TO authenticated;
GRANT ALL ON public.cart_items TO authenticated;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO authenticated;

-- Cập nhật dữ liệu cũ (nếu có)
UPDATE public.carts c SET user_auth_id = u.auth_id FROM public.users u WHERE c.user_id = u.id AND c.user_auth_id IS NULL;
