import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { VNPAY_CONFIG, verifyVnpay } from "../shared/vnpay.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  try {
    const url = new URL(req.url);
    const query = url.searchParams;

    const txnRef = query.get("vnp_TxnRef") ?? "";
    const responseCode = query.get("vnp_ResponseCode") ?? "";
    const transactionNo = query.get("vnp_TransactionNo") ?? "";
    const amount = query.get("vnp_Amount") ?? "";

    const valid = await verifyVnpay(query);
    const result = valid && responseCode === "00" ? "success" : "failed";

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const supabase = createClient(supabaseUrl, serviceKey);

    if (valid && txnRef) {
      const returnPayload = Object.fromEntries(query.entries());
      await supabase
        .from("payment_transactions")
        .update({ return_payload: returnPayload })
        .eq("provider_order_ref", txnRef)
        .eq("provider", "vnpay");
    }

    const redirectUrl =
      `${VNPAY_CONFIG.appDeepLinkBase}` +
      `?txn_ref=${encodeURIComponent(txnRef)}` +
      `&result=${encodeURIComponent(result)}` +
      `&response_code=${encodeURIComponent(responseCode)}` +
      `&transaction_no=${encodeURIComponent(transactionNo)}` +
      `&amount=${encodeURIComponent(amount)}` +
      `&checksum_valid=${encodeURIComponent(String(valid))}`;

    return Response.redirect(redirectUrl, 302);
  } catch (e) {
    return new Response(JSON.stringify({ error: String(e) }), { status: 500, headers: corsHeaders });
  }
});
