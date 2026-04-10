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
    private static final String KEY_TOKEN_EXPIRES_AT = "token_expires_at";

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

    public void saveSession(UserProfile userProfile) {
        saveSession("demo-access-token", "demo-refresh-token", userProfile, 3_600_000L);
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
                .apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
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

    public long getTokenExpiresAt() {
        return preferences.getLong(KEY_TOKEN_EXPIRES_AT, 0L);
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
                getUserId() != null ? getUserId() : "customer-01",
                getUserName() != null ? getUserName() : "Demo Customer",
                getUserEmail() != null ? getUserEmail() : "demo@foodcouriers.app",
                getUserPhone() != null ? getUserPhone() : "0900000000"
        );
    }

    public void updateSession(String accessToken, String refreshToken) {
        updateSession(accessToken, refreshToken, 3_600_000L);
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
                .remove(KEY_TOKEN_EXPIRES_AT)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();
    }
}
