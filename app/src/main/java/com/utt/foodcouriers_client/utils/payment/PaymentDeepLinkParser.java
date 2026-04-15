package com.utt.foodcouriers_client.utils.payment;

import android.net.Uri;

import com.utt.foodcouriers_client.data.model.PaymentCallbackResult;

/**
 * Parse deep link callback từ VNPAY về app.
 *
 * Deep link format:
 *   com.utt.foodcouriers.client://payment/vnpay/callback
 *     ?txn_ref=VNP-xxx
 *     &result=success|failed
 *     &response_code=00
 *     &transaction_no=12345
 *     &amount=50000000
 *     &checksum_valid=true|false
 */
public class PaymentDeepLinkParser {

    private PaymentDeepLinkParser() {}

    public static PaymentCallbackResult parse(Uri uri) {
        if (uri == null) return null;

        String txnRef       = getParam(uri, "txn_ref");
        String result       = getParam(uri, "result");
        String responseCode = getParam(uri, "response_code");
        String transactionNo= getParam(uri, "transaction_no");
        String amount       = getParam(uri, "amount");
        boolean checksumValid = "true".equalsIgnoreCase(getParam(uri, "checksum_valid"));

        return new PaymentCallbackResult(txnRef, result, responseCode, transactionNo, amount, checksumValid);
    }

    private static String getParam(Uri uri, String key) {
        String value = uri.getQueryParameter(key);
        return value != null ? value : "";
    }
}
