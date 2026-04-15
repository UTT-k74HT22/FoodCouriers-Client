ALTER TABLE public.orders
    DROP CONSTRAINT IF EXISTS orders_payment_method_check;

ALTER TABLE public.orders
    ADD CONSTRAINT orders_payment_method_check
    CHECK (payment_method IN ('cod', 'vnpay'));

ALTER TABLE public.payment_transactions
    ADD COLUMN IF NOT EXISTS provider_order_ref TEXT,
    ADD COLUMN IF NOT EXISTS gateway_transaction_no TEXT,
    ADD COLUMN IF NOT EXISTS gateway_response_code TEXT,
    ADD COLUMN IF NOT EXISTS bank_code TEXT,
    ADD COLUMN IF NOT EXISTS pay_url TEXT,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS idempotency_key TEXT,
    ADD COLUMN IF NOT EXISTS failure_reason TEXT,
    ADD COLUMN IF NOT EXISTS return_payload JSONB,
    ADD COLUMN IF NOT EXISTS ipn_payload JSONB;

CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_tx_provider_order_ref
ON public.payment_transactions(provider, provider_order_ref)
WHERE provider_order_ref IS NOT NULL;