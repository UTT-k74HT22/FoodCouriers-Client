import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { VNPAY_CONFIG, verifyVnpay } from "../_shared/vnpay.ts";

serve(async (req) => {
  try {
    const url = new URL(req.url);
    const query = url.searchParams;

    const valid = verifyVnpay(query);

    const txnRef = query.get("vnp_TxnRef") ?? "";
    const responseCode = query.get("vnp_ResponseCode") ?? "";
    const transactionNo = query.get("vnp_TransactionNo") ?? "";
    const amount = query.get("vnp_Amount") ?? "";

    const result = valid && responseCode === "00" ? "success" : "failed";

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
    return new Response(JSON.stringify({ error: String(e) }), { status: 500 });
  }
});