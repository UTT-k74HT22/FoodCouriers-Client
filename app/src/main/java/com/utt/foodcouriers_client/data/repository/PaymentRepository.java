package com.utt.foodcouriers_client.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.PaymentInitResult;
import com.utt.foodcouriers_client.data.remote.AuthClient;
import com.utt.foodcouriers_client.data.remote.SupabaseConfig;
import com.utt.foodcouriers_client.utils.SessionManager;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PaymentRepository {
    private static final String TAG = "PAYMENT_REPO";

    private static final MediaType JSON = MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON);
    private static PaymentRepository instance;

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static synchronized PaymentRepository getInstance() {
        if (instance == null) {
            instance = new PaymentRepository();
        }
        return instance;
    }

    /**
     * Gọi Edge Function create-vnpay-payment để tạo payment transaction
     * và lấy paymentUrl để mở trình duyệt.
     *
     * @param context  Context để lấy session
     * @param orderId  UUID của order vừa tạo (payment_method phải là 'vnpay')
     * @param callback Trả về PaymentInitResult chứa paymentUrl khi thành công
     */
    public void createVnpayPayment(Context context, String orderId,
                                   RepositoryCallback<PaymentInitResult> callback) {
        createVnpayPayment(context, orderId, callback, false);
    }

    private void createVnpayPayment(Context context, String orderId,
                                    RepositoryCallback<PaymentInitResult> callback,
                                    boolean hasRetriedAfterRefresh) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }

        String url = SupabaseConfig.SUPABASE_URL + "/functions/v1/create-vnpay-payment";
        Log.d(TAG, "Step 4: Calling edge function create-vnpay-payment for orderId=" + orderId);

        JsonObject body = new JsonObject();
        body.addProperty("order_id", orderId);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Step 4: Network error calling create-vnpay-payment", e);
                postError(callback, "Lỗi kết nối: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Step 4: Edge function failed HTTP " + response.code() + ", body=" + responseBody);
                    if (response.code() == 401
                            && responseBody.contains("Invalid JWT")
                            && !hasRetriedAfterRefresh) {
                        Log.w(TAG, "Step 4: JWT rejected, attempting session refresh before retry");
                        refreshSessionAndRetry(context, orderId, callback);
                        return;
                    }
                    postError(callback, parseErrorMessage(responseBody, response.code()));
                    return;
                }
                try {
                    JsonObject obj = JsonParser.parseString(responseBody).getAsJsonObject();
                    PaymentInitResult result = new PaymentInitResult(
                            getString(obj, "order_id"),
                            getString(obj, "transaction_id"),
                            getString(obj, "provider"),
                            getString(obj, "provider_order_ref"),
                            getString(obj, "payment_url"),
                            getString(obj, "expires_at")
                    );
                    Log.d(TAG, "Step 4: Edge function success txnRef=" + result.getProviderOrderRef());
                    postSuccess(callback, result);
                } catch (Exception e) {
                    Log.e(TAG, "Step 4: Failed to parse create-vnpay-payment response", e);
                    postError(callback, "Lỗi xử lý dữ liệu: " + e.getMessage());
                }
            }
        });
    }

    private void refreshSessionAndRetry(Context context, String orderId,
                                        RepositoryCallback<PaymentInitResult> callback) {
        AuthClient.getInstance().refreshSession(new AuthClient.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                String newAccessToken = AuthClient.getInstance().getAccessToken();
                String newRefreshToken = AuthClient.getInstance().getRefreshToken();
                SessionManager.getInstance(context).updateSession(newAccessToken, newRefreshToken);
                Log.d(TAG, "Step 4: Session refresh succeeded, retrying create-vnpay-payment");
                createVnpayPayment(context, orderId, callback, true);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Step 4: Session refresh failed: " + error);
                postError(callback, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            }
        });
    }

    private String parseErrorMessage(String body, int code) {
        try {
            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
            if (obj.has("error")) return obj.get("error").getAsString();
        } catch (Exception ignored) {}
        return "Tạo thanh toán VNPAY thất bại (HTTP " + code + ")";
    }

    private String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    private <T> void postSuccess(RepositoryCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private void postError(RepositoryCallback<?> callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }
}
