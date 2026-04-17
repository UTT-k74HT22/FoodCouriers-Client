import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import {
  VNPAY_CONFIG,
  formatDateVN,
  buildExpireDate,
  signVnpay,
  sortObject,
  buildQueryString,
} from "../shared/vnpay.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: "Missing Authorization header" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const body = await req.json();
    const orderId = body?.order_id;
    const clientIdempotencyKey = req.headers.get("Idempotency-Key") ?? body?.idempotency_key ?? null;

    if (!orderId) {
      return new Response(
        JSON.stringify({ error: "order_id is required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";

    // Pattern đúng cho Supabase Edge Functions:
    // Dùng anon key + override Authorization = user JWT để verify identity.
    // Không dùng service role key để verify user vì có thể conflict.
    const supabaseUser = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authHeader } },
      auth: { persistSession: false },
    });

    const { data: { user }, error: userError } = await supabaseUser.auth.getUser();

    if (userError || !user) {
      return new Response(
        JSON.stringify({ error: "Unauthorized", detail: userError?.message ?? "User not found" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Service role client cho tất cả thao tác DB (bypass RLS)
    const supabase = createClient(supabaseUrl, serviceKey);

    const { data: publicUser, error: publicUserError } = await supabase
      .from("users")
      .select("id")
      .eq("auth_id", user.id)
      .single();

    if (publicUserError || !publicUser) {
      return new Response(
        JSON.stringify({ error: "Customer profile not found" }),
        { status: 404, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const { data: order, error: orderError } = await supabase
      .from("orders")
      .select("*")
      .eq("id", orderId)
      .eq("user_id", publicUser.id)
      .single();

    if (orderError || !order) {
      return new Response(
        JSON.stringify({ error: "Order not found" }),
        { status: 404, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (order.payment_method !== "vnpay") {
      return new Response(
        JSON.stringify({ error: "Order payment_method must be vnpay" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (order.payment_status === "paid") {
      return new Response(
        JSON.stringify({ error: "Order already paid" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (order.status === "cancelled") {
      return new Response(
        JSON.stringify({ error: "Order is cancelled" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Check for existing pending transaction - return existing if not expired
    const { data: existingTx } = await supabase
      .from("payment_transactions")
      .select("*")
      .eq("order_id", order.id)
      .eq("status", "pending")
      .single();

    // Check idempotency - if same key exists, return existing transaction
    if (clientIdempotencyKey) {
      const { data: idempotentTx } = await supabase
        .from("payment_transactions")
        .select("*")
        .eq("order_id", order.id)
        .eq("idempotency_key", clientIdempotencyKey)
        .single();
      
      if (idempotentTx) {
        return new Response(
          JSON.stringify({
            order_id: order.id,
            transaction_id: idempotentTx.id,
            provider: "vnpay",
            provider_order_ref: idempotentTx.provider_order_ref,
            payment_url: idempotentTx.pay_url,
            expires_at: idempotentTx.expires_at,
            reused: true,
            idempotent: true,
          }),
          { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
    }

    if (existingTx) {
      const expiresAt = new Date(existingTx.expires_at);
      const now = new Date();
      
      // If existing transaction not expired, return it
      if (expiresAt > now && existingTx.pay_url) {
        return new Response(
          JSON.stringify({
            order_id: order.id,
            transaction_id: existingTx.id,
            provider: "vnpay",
            provider_order_ref: existingTx.provider_order_ref,
            payment_url: existingTx.pay_url,
            expires_at: existingTx.expires_at,
            reused: true,
          }),
          { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
      
      // If expired, mark as failed and create new one
      if (expiresAt <= now) {
        await supabase
          .from("payment_transactions")
          .update({ status: "failed", failure_reason: "Payment expired without completion" })
          .eq("id", existingTx.id);
      }
    }

    const now = new Date();
    const createDate = formatDateVN(now);
    const expireDate = buildExpireDate(now, 15);

    const providerOrderRef = `VNP-${order.id.replace(/-/g, "").slice(0, 20)}-${Date.now()}`;

    const amount = Math.round(Number(order.total || 0));
    if (!amount || amount <= 0) {
      return new Response(
        JSON.stringify({ error: "Invalid order amount", total: order.total }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const txnParams: Record<string, string> = {
      vnp_Version: VNPAY_CONFIG.version,
      vnp_Command: VNPAY_CONFIG.command,
      vnp_TmnCode: VNPAY_CONFIG.tmnCode,
      vnp_Amount: String(amount * 100),
      vnp_CurrCode: VNPAY_CONFIG.currCode,
      vnp_TxnRef: providerOrderRef,
      vnp_OrderInfo: `Thanh toan don hang ${order.id}`,
      vnp_OrderType: VNPAY_CONFIG.orderType,
      vnp_Locale: VNPAY_CONFIG.locale,
      vnp_ReturnUrl: VNPAY_CONFIG.returnUrl,
      vnp_IpAddr: req.headers.get("X-Forwarded-For") ?? "127.0.0.1",
      vnp_CreateDate: createDate,
      vnp_ExpireDate: expireDate,
    };

    if (VNPAY_CONFIG.ipnUrl) {
      txnParams.vnp_IpnUrl = VNPAY_CONFIG.ipnUrl;
    }

    const sortedParams = sortObject(txnParams);
    const secureHash = await signVnpay(sortedParams);
    const paymentUrl = `${VNPAY_CONFIG.paymentUrl}?${buildQueryString(sortedParams)}&vnp_SecureHash=${secureHash}`;

    const { data: transaction, error: txError } = await supabase
      .from("payment_transactions")
      .insert({
        order_id: order.id,
        provider: "vnpay",
        provider_order_ref: providerOrderRef,
        amount,
        status: "pending",
        pay_url: paymentUrl,
        expires_at: new Date(now.getTime() + 15 * 60 * 1000).toISOString(),
        idempotency_key: clientIdempotencyKey,
      })
      .select()
      .single();

    if (txError) {
      return new Response(
        JSON.stringify({ error: txError.message }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({
        order_id: order.id,
        transaction_id: transaction.id,
        provider: "vnpay",
        provider_order_ref: providerOrderRef,
        payment_url: paymentUrl,
        expires_at: transaction.expires_at,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (e) {
    return new Response(
      JSON.stringify({ error: String(e) }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
