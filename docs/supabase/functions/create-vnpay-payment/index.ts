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

serve(async (req) => {
  try {
    if (req.method !== "POST") {
      return new Response(JSON.stringify({ error: "Method not allowed" }), { status: 405 });
    }

    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(JSON.stringify({ error: "Missing Authorization header" }), { status: 401 });
    }

    const body = await req.json();
    const orderId = body?.order_id;

    if (!orderId) {
      return new Response(JSON.stringify({ error: "order_id is required" }), { status: 400 });
    }

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const jwt = authHeader.replace("Bearer ", "");
    const {
      data: { user },
      error: userError,
    } = await supabase.auth.getUser(jwt);

    if (userError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 });
    }

    const { data: order, error: orderError } = await supabase
      .from("orders")
      .select("*")
      .eq("id", orderId)
      .eq("user_id", user.id)
      .single();

    if (orderError || !order) {
      return new Response(JSON.stringify({ error: "Order not found" }), { status: 404 });
    }

    if (order.payment_method !== "vnpay") {
      return new Response(JSON.stringify({ error: "Order payment_method must be vnpay" }), { status: 400 });
    }

    if (order.payment_status === "paid") {
      return new Response(JSON.stringify({ error: "Order already paid" }), { status: 400 });
    }

    if (order.status === "cancelled") {
      return new Response(JSON.stringify({ error: "Order is cancelled" }), { status: 400 });
    }

    const now = new Date();
    const createDate = formatDateVN(now);
    const expireDate = buildExpireDate(now, 15);

    const providerOrderRef = `VNP-${order.id.replace(/-/g, "").slice(0, 20)}-${Date.now()}`;

    const amount = Math.round(Number(order.total_amount || order.final_amount || 0));
    if (!amount || amount <= 0) {
      return new Response(JSON.stringify({ error: "Invalid order amount" }), { status: 400 });
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
      vnp_IpAddr: "127.0.0.1",
      vnp_CreateDate: createDate,
      vnp_ExpireDate: expireDate,
    };

    const sortedParams = sortObject(txnParams);
    const secureHash = signVnpay(sortedParams);
    const paymentUrl = `${VNPAY_CONFIG.paymentUrl}?${buildQueryString(sortedParams)}&vnp_SecureHash=${secureHash}`;

    const { data: transaction, error: txError } = await supabase
      .from("payment_transactions")
      .insert({
        order_id: order.id,
        provider: "vnpay",
        provider_order_ref: providerOrderRef,
        status: "pending",
        pay_url: paymentUrl,
        expires_at: new Date(now.getTime() + 15 * 60 * 1000).toISOString(),
      })
      .select()
      .single();

    if (txError) {
      return new Response(JSON.stringify({ error: txError.message }), { status: 500 });
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
      {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }
    );
  } catch (e) {
    return new Response(JSON.stringify({ error: String(e) }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }
});