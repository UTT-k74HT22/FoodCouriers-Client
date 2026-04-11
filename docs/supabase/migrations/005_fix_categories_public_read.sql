-- =====================================================
-- Fix categories RLS for client public read
-- Version: 1.1
-- Date: 2026-04-11
-- Description:
--   - Allow anon/authenticated to read active categories
--   - Avoid SELECT policy on categories depending on public.users
--   - Keep admin-only write policies
-- =====================================================

BEGIN;

-- Ensure API roles can access schema/table metadata needed for REST reads.
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT SELECT ON TABLE public.categories TO anon, authenticated;

-- Remove the broad policy that also affected SELECT via users table lookup.
DROP POLICY IF EXISTS "categories_manage" ON public.categories;
DROP POLICY IF EXISTS "categories_select" ON public.categories;
DROP POLICY IF EXISTS "categories_insert_admin" ON public.categories;
DROP POLICY IF EXISTS "categories_update_admin" ON public.categories;
DROP POLICY IF EXISTS "categories_delete_admin" ON public.categories;

-- Public read for active categories.
CREATE POLICY "categories_select" ON public.categories
    FOR SELECT
    TO anon, authenticated
    USING (is_active = true);

-- Admin write policies only.
CREATE POLICY "categories_insert_admin" ON public.categories
    FOR INSERT
    TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1
            FROM public.users
            WHERE auth_id = auth.uid() AND role = 'admin'
        )
    );

CREATE POLICY "categories_update_admin" ON public.categories
    FOR UPDATE
    TO authenticated
    USING (
        EXISTS (
            SELECT 1
            FROM public.users
            WHERE auth_id = auth.uid() AND role = 'admin'
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1
            FROM public.users
            WHERE auth_id = auth.uid() AND role = 'admin'
        )
    );

CREATE POLICY "categories_delete_admin" ON public.categories
    FOR DELETE
    TO authenticated
    USING (
        EXISTS (
            SELECT 1
            FROM public.users
            WHERE auth_id = auth.uid() AND role = 'admin'
        )
    );

COMMIT;
