-- 010_fix_vnpay_payment_transactions_rls.sql
-- Align payment_transactions RLS with the fact that orders.user_id references public.users.id
-- while auth.uid() returns auth.users.id.

ALTER TABLE IF EXISTS public.payment_transactions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "payment_transactions_select_own" ON public.payment_transactions;
CREATE POLICY "payment_transactions_select_own" ON public.payment_transactions
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1
      FROM public.orders o
      JOIN public.users u ON u.id = o.user_id
      WHERE o.id = payment_transactions.order_id
        AND u.auth_id = auth.uid()
    )
  );
