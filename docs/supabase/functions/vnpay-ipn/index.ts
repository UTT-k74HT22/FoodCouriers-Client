import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { verifyVnpay } from "../_shared/vnpay.ts";

serve(async (req) => {
  try {
    const url = new URL(req.url);
    const query = url.searchParams;

    const valid = verifyVnpay(query);
    if (!valid) {
      return new Response(JSON.stringify({ RspCode: "97", Message: "Invalid checksum" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }

    const txnRef = query.get("vnp_TxnRef");
    const responseCode = query.get("vnp_ResponseCode") ?? "";
    const transactionNo = query.get("vnp_TransactionNo") ?? "";
    const bankCode = query.get("vnp_BankCode") ?? "";
    const payDate = query.get("vnp_PayDate") ?? "";

    if (!txnRef) {
      return new Response(JSON.stringify({ RspCode: "01", Message: "TxnRef not found" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const { data: tx, error: txError } = await supabase
      .from("payment_transactions")
      .select("*")
      .eq("provider", "vnpay")
      .eq("provider_order_ref", txnRef)
      .single();

    if (txError || !tx) {
      return new Response(JSON.stringify({ RspCode: "01", Message: "Transaction not found" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }

    if (tx.status === "success") {
      return new Response(JSON.stringify({ RspCode: "00", Message: "Confirm Success" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }

    const ipnPayload = Object.fromEntries(query.entries());

    if (responseCode === "00") {
      await supabase
        .from("payment_transactions")
        .update({
          status: "success",
          gateway_transaction_no: transactionNo,
          gateway_response_code: responseCode,
          bank_code: bankCode,
          paid_at: new Date().toISOString(),
          ipn_payload: ipnPayload,
        })
        .eq("id", tx.id);

      await supabase
        .from("orders")
        .update({
          payment_status: "paid",
        })
        .eq("id", tx.order_id);

      return new Response(JSON.stringify({ RspCode: "00", Message: "Confirm Success" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }

    await supabase
      .from("payment_transactions")
      .update({
        status: "failed",
        gateway_transaction_no: transactionNo,
        gateway_response_code: responseCode,
        bank_code: bankCode,
        failure_reason: `VNPAY response code ${responseCode}`,
        ipn_payload: ipnPayload,
      })
      .eq("id", tx.id);

    await supabase
      .from("orders")
      .update({
        payment_status: "failed",
      })
      .eq("id", tx.order_id);

    return new Response(JSON.stringify({ RspCode: "00", Message: "Confirm Success" }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  } catch (e) {
    return new Response(JSON.stringify({ RspCode: "99", Message: String(e) }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  }
});