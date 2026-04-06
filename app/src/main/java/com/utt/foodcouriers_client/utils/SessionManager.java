package com.utt.foodcouriers_client.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.utt.foodcouriers_client.data.remote.AuthSession;

public class SessionManager {
    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = createPreferences(context.getApplicationContext());
    }

    public void saveSession(AuthSession session) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, session.getAccessToken())
                .putString(KEY_REFRESH_TOKEN, session.getRefreshToken())
                .putLong(KEY_EXPIRES_AT, session.getExpiresAt())
                .putString(KEY_USER_ID, session.getUserId())
                .putString(KEY_EMAIL, session.getEmail())
                .apply();
    }

    public AuthSession getSession() {
        String accessToken = prefs.getString(KEY_ACCESS_TOKEN, null);
        String refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null);
        long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L);
        String userId = prefs.getString(KEY_USER_ID, null);
        String email = prefs.getString(KEY_EMAIL, null);

        if (accessToken == null || refreshToken == null) {
            return null;
        }

        return new AuthSession(accessToken, refreshToken, expiresAt, userId, email);
    }

    public boolean hasValidSession() {
        AuthSession session = getSession();
        return session != null && System.currentTimeMillis() < session.getExpiresAt();
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }

    private SharedPreferences createPreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception ignored) {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }
}
