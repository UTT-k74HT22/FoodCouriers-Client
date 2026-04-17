package com.utt.foodcouriers_client;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.utt.foodcouriers_client.data.remote.AuthClient;
import com.utt.foodcouriers_client.data.remote.SupabaseRealtimeClient;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.ToastBanner;
import com.utt.foodcouriers_client.utils.websocket.RealtimeChannel;
import com.utt.foodcouriers_client.utils.websocket.RealtimeListener;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Application class khởi tạo tài nguyên dùng chung của app.
 *
 * <p>Phần quan trọng nhất với auth/realtime là khôi phục session khi app mở lại:</p>
 * <ol>
 *     <li>Đọc session đã lưu trong {@link SessionManager}.</li>
 *     <li>Nếu token hết hạn và có refresh token, gọi {@link AuthClient#refreshSession(AuthClient.ApiCallback)}.</li>
 *     <li>Khi có access token hợp lệ, gọi {@link #initializeRealtime(String)} để mở WebSocket.</li>
 *     <li>Subscribe notification toàn app để hiện toast và báo các màn refresh badge.</li>
 * </ol>
 */
public class FoodCouriersClientApp extends Application {

    private static final String TAG = "FoodCouriersApp";
    private static Context appContext;
    private static RealtimeChannel appNotificationChannel;
    private static String appNotificationUserId;
    private static final Set<Runnable> notificationRefreshListeners = new CopyOnWriteArraySet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        ToastBanner.init(this);

        SessionManager sessionManager = SessionManager.getInstance(this);
        if (sessionManager.isLoggedIn()) {
            if (sessionManager.isTokenExpired()) {
                Log.d(TAG, "Token expired, attempting refresh...");
                if (sessionManager.hasRefreshToken()) {
                    AuthClient.getInstance().setSession(
                            sessionManager.getAccessToken(),
                            sessionManager.getRefreshToken()
                    );
                    AuthClient.getInstance().refreshSession(new AuthClient.ApiCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            Log.d(TAG, "Token refresh successful");
                            String refreshedAccessToken = AuthClient.getInstance().getAccessToken();
                            SessionManager.getInstance(FoodCouriersClientApp.this).updateSession(
                                    refreshedAccessToken,
                                    AuthClient.getInstance().getRefreshToken()
                            );
                            initializeRealtime(refreshedAccessToken);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Token refresh failed: " + error);
                            AuthClient.getInstance().clearSession();
                            sessionManager.clearSession();
                        }
                    });
                } else {
                    Log.d(TAG, "No refresh token, clearing session");
                    AuthClient.getInstance().clearSession();
                    sessionManager.clearSession();
                }
            } else {
                AuthClient.getInstance().setSession(
                        sessionManager.getAccessToken(),
                        sessionManager.getRefreshToken()
                );
                initializeRealtime(sessionManager.getAccessToken());
            }
        }
    }

    /**
     * Khởi tạo Supabase Realtime sau khi app có access token hợp lệ.
     *
     * <p>Hàm này được gọi sau email/password login, Google OAuth bootstrap,
     * register thành công hoặc refresh token khi app khởi động lại. Client realtime
     * giữ application context để kiểm tra network, mở WebSocket, sau đó subscribe
     * notification toàn app.</p>
     *
     * @param accessToken access token Supabase Auth của user hiện tại
     */
    public static void initializeRealtime(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return;
        }
        SupabaseRealtimeClient client = SupabaseRealtimeClient.getInstance();
        client.initialize(appContext, accessToken);
        client.connect();
        subscribeAppNotifications();
    }

    /**
     * Ngắt toàn bộ realtime khi user logout hoặc app cần clear session.
     */
    public static void disconnectRealtime() {
        if (appNotificationChannel != null) {
            SupabaseRealtimeClient.getInstance().unsubscribe(appNotificationChannel);
            appNotificationChannel = null;
            appNotificationUserId = null;
        }
        SupabaseRealtimeClient.getInstance().disconnect();
    }

    /**
     * Subscribe notification ở cấp Application để app nhận thông báo dù user không mở màn list.
     *
     * <p>Subscription này lọc theo {@code user_id}. Khi có INSERT, app hiện toast bằng nội dung
     * notification và gọi {@link #notifyNotificationChanged()} để các màn đang active refresh badge.</p>
     */
    private static void subscribeAppNotifications() {
        if (appContext == null) {
            return;
        }

        String userId = SessionManager.getInstance(appContext).getUserId();
        if (userId == null || userId.isBlank()) {
            return;
        }

        if (appNotificationChannel != null && userId.equals(appNotificationUserId)) {
            return;
        }

        if (appNotificationChannel != null) {
            SupabaseRealtimeClient.getInstance().unsubscribe(appNotificationChannel);
            appNotificationChannel = null;
        }

        appNotificationUserId = userId;
        appNotificationChannel = SupabaseRealtimeClient.getInstance()
                .subscribe("public:notifications", "user_id=eq." + userId, new RealtimeListener() {
                    @Override
                    public void onInsert(JsonObject record) {
                        String title = getString(record, "title");
                        String body = getString(record, "body");
                        String message = !body.isBlank() ? body : title;
                        if (!message.isBlank()) {
                            ToastBanner.showSuccess(message);
                        }
                        notifyNotificationChanged();
                    }

                    @Override
                    public void onUpdate(JsonObject record, JsonObject oldRecord) {
                        notifyNotificationChanged();
                    }

                    @Override
                    public void onDelete(JsonObject oldRecord) {
                        notifyNotificationChanged();
                    }

                    @Override
                    public void onConnected() {
                        Log.d(TAG, "App notification realtime connected for user: " + userId);
                    }

                    @Override
                    public void onError(String error) {
                        if (error != null && error.contains("temporarily unavailable")) {
                            Log.w(TAG, "App notification realtime reconnecting: " + error);
                        } else {
                            Log.e(TAG, "App notification realtime error: " + error);
                        }
                    }
                });
    }

    /**
     * Đăng ký listener refresh badge/list khi notification realtime thay đổi.
     *
     * @param listener runnable thường được Activity đưa về main thread trước khi update UI
     */
    public static void addNotificationRefreshListener(Runnable listener) {
        if (listener != null) {
            notificationRefreshListeners.add(listener);
        }
    }

    /**
     * Gỡ listener đã đăng ký để tránh callback sau khi Activity/Fragment dừng.
     *
     * @param listener listener cần gỡ
     */
    public static void removeNotificationRefreshListener(Runnable listener) {
        if (listener != null) {
            notificationRefreshListeners.remove(listener);
        }
    }

    /**
     * Phát tín hiệu notification thay đổi cho các listener trong app.
     */
    private static void notifyNotificationChanged() {
        for (Runnable listener : notificationRefreshListeners) {
            listener.run();
        }
    }

    /**
     * Đọc string an toàn từ payload realtime.
     *
     * @param object record JSON từ Supabase
     * @param key tên field cần đọc
     * @return giá trị string hoặc chuỗi rỗng nếu field thiếu/null
     */
    private static String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }
}
