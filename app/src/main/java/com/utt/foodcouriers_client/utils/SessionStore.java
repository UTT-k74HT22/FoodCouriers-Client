package com.utt.foodcouriers_client.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.utt.foodcouriers_client.data.auth.SocialAuthResult;

public class SessionStore {
    private static final String PREF_NAME = "supabase_session_pref";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_PROVIDER = "provider";
    private static final String KEY_RAW_URI = "raw_uri";

    private SessionStore() {
    }

    public static void saveSession(@NonNull Context context, @NonNull SocialAuthResult result) {
        long expiresAt = 0L;
        if (result.getExpiresInSeconds() > 0) {
            expiresAt = System.currentTimeMillis() + (result.getExpiresInSeconds() * 1000L);
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, result.getAccessToken())
                .putString(KEY_REFRESH_TOKEN, result.getRefreshToken())
                .putLong(KEY_EXPIRES_AT, expiresAt)
                .putString(KEY_PROVIDER, result.getProvider() != null ? result.getProvider().getValue() : null)
                .putString(KEY_RAW_URI, result.getRawUri())
                .apply();
    }

    public static void clearSession(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public static boolean hasSession(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String accessToken = prefs.getString(KEY_ACCESS_TOKEN, null);
        String refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null);
        return notBlank(accessToken) && notBlank(refreshToken);
    }

    @Nullable
    public static String getAccessToken(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    @Nullable
    public static String getRefreshToken(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public static long getExpiresAt(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_EXPIRES_AT, 0L);
    }

    @Nullable
    public static String getProvider(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PROVIDER, null);
    }

    @Nullable
    public static String getRawUri(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_RAW_URI, null);
    }

    public static long getExpiresInSeconds(@NonNull Context context) {
        long expiresAt = getExpiresAt(context);
        if (expiresAt <= 0L) {
            return 0L;
        }

        long remainingMillis = expiresAt - System.currentTimeMillis();
        if (remainingMillis <= 0L) {
            return 0L;
        }

        return remainingMillis / 1000L;
    }

    private static boolean notBlank(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }
}
