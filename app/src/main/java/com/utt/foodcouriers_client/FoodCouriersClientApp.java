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

public class FoodCouriersClientApp extends Application {

    private static final String TAG = "FoodCouriersApp";
    private static Context appContext;
    private static RealtimeChannel appNotificationChannel;
    private static String appNotificationUserId;

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

    public static void initializeRealtime(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return;
        }
        SupabaseRealtimeClient client = SupabaseRealtimeClient.getInstance();
        client.initialize(accessToken);
        client.connect();
        subscribeAppNotifications();
    }

    public static void disconnectRealtime() {
        if (appNotificationChannel != null) {
            SupabaseRealtimeClient.getInstance().unsubscribe(appNotificationChannel);
            appNotificationChannel = null;
            appNotificationUserId = null;
        }
        SupabaseRealtimeClient.getInstance().disconnect();
    }

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
                    }

                    @Override
                    public void onUpdate(JsonObject record, JsonObject oldRecord) {
                    }

                    @Override
                    public void onDelete(JsonObject oldRecord) {
                    }

                    @Override
                    public void onConnected() {
                        Log.d(TAG, "App notification realtime connected for user: " + userId);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "App notification realtime error: " + error);
                    }
                });
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }
}
