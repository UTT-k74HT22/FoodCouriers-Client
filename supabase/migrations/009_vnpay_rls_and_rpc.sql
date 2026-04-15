-- 009_vnpay_rls_and_rpc.sql
-- RLS policies and RPC functions for VNPAY payment integration

-- Enable RLS on payment_transactions if not already
ALTER TABLE IF EXISTS public.payment_transactions ENABLE ROW LEVEL SECURITY;

-- Policy: Users can select their own payment transactions
DROP POLICY IF EXISTS "payment_transactions_select_own" ON public.payment_transactions;
CREATE POLICY "payment_transactions_select_own" ON public.payment_transactions
  FOR SELECT
  USING (
    order_id IN (
      SELECT id FROM public.orders WHERE user_id = auth.uid()
    )
  );

-- Policy: Service role can do all operations on payment_transactions
DROP POLICY IF EXISTS "payment_transactions_service_role_all" ON public.payment_transactions;
CREATE POLICY "payment_transactions_service_role_all" ON public.payment_transactions
  FOR ALL
  USING (auth.role() = 'service_role');

-- RPC: Mark order payment as success (called by IPN edge function)
CREATE OR REPLACE FUNCTION public.rpc_mark_order_payment_success(
  p_order_id UUID,
  p_transaction_id UUID,
  p_gateway_txn_no TEXT DEFAULT NULL,
  p_response_code TEXT DEFAULT '00',
  p_bank_code TEXT DEFAULT NULL
)
RETURNS VOID
SECURITY DEFINER
SET search_path = public
LANGUAGE plpgsql
AS $$
BEGIN
  UPDATE public.payment_transactions
  SET status = 'success',
      gateway_transaction_no = p_gateway_txn_no,
      gateway_response_code = p_response_code,
      bank_code = p_bank_code,
      paid_at = NOW()
  WHERE id = p_transaction_id AND order_id = p_order_id;

  UPDATE public.orders
  SET payment_status = 'paid'
  WHERE id = p_order_id AND payment_status != 'paid';
END;
$$;

-- RPC: Mark order payment as failed (called by IPN edge function)
CREATE OR REPLACE FUNCTION public.rpc_mark_order_payment_failed(
  p_order_id UUID,
  p_transaction_id UUID,
  p_response_code TEXT DEFAULT NULL,
  p_failure_reason TEXT DEFAULT NULL
)
RETURNS VOID
SECURITY DEFINER
SET search_path = public
LANGUAGE plpgsql
AS $$
BEGIN
  UPDATE public.payment_transactions
  SET status = 'failed',
      gateway_response_code = p_response_code,
      failure_reason = p_failure_reason
  WHERE id = p_transaction_id AND order_id = p_order_id;

  UPDATE public.orders
  SET payment_status = 'failed'
  WHERE id = p_order_id;
END;
$$;

-- Grant execute to anon and authenticated roles
GRANT EXECUTE ON FUNCTION public.rpc_mark_order_payment_success(UUID, UUID, TEXT, TEXT, TEXT) TO service_role;
GRANT EXECUTE ON FUNCTION public.rpc_mark_order_payment_failed(UUID, UUID, TEXT, TEXT) TO service_role;