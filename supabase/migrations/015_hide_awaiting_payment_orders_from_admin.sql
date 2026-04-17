-- Keep unpaid VNPAY orders invisible to admin/restaurant processing screens.
-- The customer can still see their own awaiting_payment order.

DROP POLICY IF EXISTS orders_select_v3 ON public.orders;

CREATE POLICY orders_select_v3
ON public.orders
FOR SELECT
TO authenticated
USING (
    (
        private.get_current_user_role() = 'admin'
        AND status <> 'awaiting_payment'
    )
    OR user_id = private.get_current_user_id()
    OR (
        private.get_current_user_role() = 'shipper'
        AND (
            shipper_id = private.get_current_user_id()
            OR (
                shipper_id IS NULL
                AND status = ANY (ARRAY['confirmed'::text, 'preparing'::text, 'ready_for_pickup'::text])
            )
        )
    )
);

CREATE OR REPLACE FUNCTION public.rpc_mark_order_payment_success(
    p_order_id uuid,
    p_transaction_id uuid,
    p_gateway_txn_no text DEFAULT NULL::text,
    p_response_code text DEFAULT '00'::text,
    p_bank_code text DEFAULT NULL::text
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
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
  SET payment_status = 'paid',
      status = CASE WHEN status = 'awaiting_payment' THEN 'pending' ELSE status END
  WHERE id = p_order_id AND payment_status != 'paid';
END;
$$;

CREATE OR REPLACE FUNCTION public.rpc_create_order(
    p_user_id uuid,
    p_restaurant_id uuid,
    p_delivery_address text,
    p_delivery_latitude double precision,
    p_delivery_longitude double precision,
    p_note text,
    p_payment_method text,
    p_promotion_code text,
    p_items jsonb
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
BEGIN
    RETURN public.rpc_create_order(
        p_user_id,
        p_restaurant_id,
        p_delivery_address,
        p_delivery_latitude,
        p_delivery_longitude,
        p_note,
        p_payment_method,
        p_promotion_code,
        p_items,
        NULL::integer
    );
END;
$$;
