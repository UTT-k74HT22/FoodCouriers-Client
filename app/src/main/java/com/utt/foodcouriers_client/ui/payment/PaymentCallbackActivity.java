package com.utt.foodcouriers_client.ui.payment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.utt.foodcouriers_client.data.model.PaymentCallbackResult;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.main.MainActivity;
import com.utt.foodcouriers_client.ui.order.OrderDetailActivity;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.payment.PaymentDeepLinkParser;

/**
 * Activity nhận deep link callback từ VNPAY sau khi người dùng thanh toán.
 *
 * Deep link: com.utt.foodcouriers.client://payment/vnpay/callback?...
 *
 * Luồng:
 *  1. Server (vnpay-return) verify checksum rồi redirect về deep link này.
 *  2. Android mở PaymentCallbackActivity qua intent-filter.
 *  3. Activity parse params, đọc orderId từ SessionManager, navigate sang OrderDetailActivity.
 *
 * Lưu ý: IPN là nguồn sự thật cuối cùng (server-to-server). Activity này chỉ phục vụ UX.
 */
public class PaymentCallbackActivity extends BaseActivity {
    private static final String TAG = "CheckoutFlow";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        PaymentCallbackResult callbackResult = PaymentDeepLinkParser.parse(uri);
        Log.d(TAG, "Step 6: Received callback uri=" + uri);

        String orderId = SessionManager.getInstance(this).getPendingPaymentOrderId();

        if (callbackResult == null) {
            Log.w(TAG, "Step 6: Cannot parse callback, keep pending orderId=" + orderId);
            // Không parse được deep link — về màn home
            navigateHome();
            return;
        }

        Log.d(TAG, "Step 6: Parsed callback txnRef=" + callbackResult.getTxnRef()
                + ", result=" + callbackResult.getResult()
                + ", checksumValid=" + callbackResult.isChecksumValid());

        if (callbackResult.isSuccess()) {
            showSuccessBanner("Thanh toán thành công!");
        } else {
            showWarningBanner("Thanh toán không thành công. Vui lòng thử lại.");
        }

        if (orderId != null && !orderId.isEmpty()) {
            SessionManager.getInstance(this).clearPendingPaymentOrderId();
            Log.d(TAG, "Step 7: Navigate to order detail orderId=" + orderId);
            navigateToOrderDetail(orderId);
        } else {
            Log.w(TAG, "Step 7: Missing pending orderId, fallback to home");
            // Không có orderId — về home để user tự tìm đơn trong lịch sử
            navigateHome();
        }
    }

    private void navigateToOrderDetail(String orderId) {
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, orderId);
        intent.putExtra(OrderDetailActivity.EXTRA_FROM_PAYMENT_CALLBACK, true);
        // Xóa stack: CheckoutActivity đã finish(), không cần back về đó
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void navigateHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
