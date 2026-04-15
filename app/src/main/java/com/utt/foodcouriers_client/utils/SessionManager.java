package com.utt.foodcouriers_client.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.utt.foodcouriers_client.data.model.UserProfile;

public class SessionManager {

    private static final String PREF_NAME = "FoodCouriersClientPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_AVATAR = "user_avatar";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_TOKEN_EXPIRES_AT = "token_expires_at";
    private static final String KEY_CART_ID = "cart_id";
    private static final String KEY_PENDING_PAYMENT_ORDER_ID = "pending_payment_order_id";

    private static SessionManager instance;
    private final SharedPreferences preferences;

    private SessionManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void saveSession(String accessToken, String refreshToken, UserProfile userProfile, long expiresInMillis) {
        preferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_USER_ID, userProfile.getId())
                .putString(KEY_USER_EMAIL, userProfile.getEmail())
                .putString(KEY_USER_NAME, userProfile.getFullName())
                .putString(KEY_USER_PHONE, userProfile.getPhone())
                .putString(KEY_USER_AVATAR, userProfile.getAvatarUrl())
                .putString(KEY_USER_ROLE, userProfile.getRole())
                .putLong(KEY_TOKEN_EXPIRES_AT, System.currentTimeMillis() + expiresInMillis)
                .apply();
    }

    public void updateUserInfo(UserProfile userProfile) {
        if (userProfile == null) {
            return;
        }
        preferences.edit()
                .putString(KEY_USER_ID, userProfile.getId())
                .putString(KEY_USER_EMAIL, userProfile.getEmail())
                .putString(KEY_USER_NAME, userProfile.getFullName())
                .putString(KEY_USER_PHONE, userProfile.getPhone())
                .putString(KEY_USER_AVATAR, userProfile.getAvatarUrl())
                .putString(KEY_USER_ROLE, userProfile.getRole())
                .apply();
    }

    public boolean isLoggedIn() {
        if (!preferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return false;
        }
        if (isTokenExpired()) {
            clearSession();
            return false;
        }
        return true;
    }

    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return preferences.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUserId() {
        return preferences.getString(KEY_USER_ID, null);
    }

    public String getUserEmail() {
        return preferences.getString(KEY_USER_EMAIL, null);
    }

    public String getUserName() {
        return preferences.getString(KEY_USER_NAME, null);
    }

    public String getUserPhone() {
        return preferences.getString(KEY_USER_PHONE, null);
    }

    public String getUserAvatar() {
        return preferences.getString(KEY_USER_AVATAR, null);
    }

    public String getUserRole() {
        return preferences.getString(KEY_USER_ROLE, null);
    }

    public long getTokenExpiresAt() {
        return preferences.getLong(KEY_TOKEN_EXPIRES_AT, 0L);
    }

    public String getCartId() {
        return preferences.getString(KEY_CART_ID, null);
    }

    public void setCartId(String cartId) {
        preferences.edit().putString(KEY_CART_ID, cartId).apply();
    }

    /** Lưu order_id đang chờ callback VNPAY để PaymentCallbackActivity đọc lại */
    public void setPendingPaymentOrderId(String orderId) {
        preferences.edit().putString(KEY_PENDING_PAYMENT_ORDER_ID, orderId).apply();
    }

    public String getPendingPaymentOrderId() {
        return preferences.getString(KEY_PENDING_PAYMENT_ORDER_ID, null);
    }

    public void clearPendingPaymentOrderId() {
        preferences.edit().remove(KEY_PENDING_PAYMENT_ORDER_ID).apply();
    }

    public boolean isTokenExpired() {
        long expiresAt = getTokenExpiresAt();
        return expiresAt == 0L || System.currentTimeMillis() >= expiresAt;
    }

    public UserProfile getCurrentUser() {
        if (!isLoggedIn()) {
            return null;
        }
        return new UserProfile(
                getUserId(),
                getUserName(),
                getUserEmail(),
                getUserPhone()
        );
    }

    public void updateSession(String accessToken, String refreshToken) {
        updateSession(accessToken, refreshToken, 3600000L);
    }

    public void updateSession(String accessToken, String refreshToken, long expiresInMillis) {
        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_TOKEN_EXPIRES_AT, System.currentTimeMillis() + expiresInMillis)
                .apply();
    }

    public void clearSession() {
        preferences.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_PHONE)
                .remove(KEY_USER_AVATAR)
                .remove(KEY_USER_ROLE)
                .remove(KEY_TOKEN_EXPIRES_AT)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();
    }
}
