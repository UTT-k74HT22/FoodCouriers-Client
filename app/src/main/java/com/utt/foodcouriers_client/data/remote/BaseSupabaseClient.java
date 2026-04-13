package com.utt.foodcouriers_client.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public abstract class BaseSupabaseClient {

    protected static final String TAG = "BaseSupabaseClient";

    protected final OkHttpClient client;
    protected final Gson gson;
    protected final Handler mainHandler;

    protected String accessToken;
    protected String refreshToken;

    protected BaseSupabaseClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new GsonBuilder().setLenient().create();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public void setSession(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public void clearSession() {
        accessToken = null;
        refreshToken = null;
    }

    public boolean isAuthenticated() {
        return !isNullOrBlank(accessToken);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    protected String parseAuthError(String json) {
        try {
            AuthError error = gson.fromJson(json, AuthError.class);
            if (error != null) {
                if (!isNullOrBlank(error.getMessage())) {
                    return error.getMessage();
                }
                if (!isNullOrBlank(error.getMsg())) {
                    return error.getMsg();
                }
                if (!isNullOrBlank(error.getErrorDescription())) {
                    return error.getErrorDescription();
                }
            }
        } catch (Exception exception) {
            Log.e(TAG, "Failed to parse auth error", exception);
        }
        return "Authentication failed. Please check your credentials.";
    }

    protected String parseRestError(String fallbackMessage, int statusCode, String json) {
        try {
            AuthError error = gson.fromJson(json, AuthError.class);
            if (error != null) {
                if (!isNullOrBlank(error.getMessage())) {
                    return error.getMessage();
                }
                if (!isNullOrBlank(error.getMsg())) {
                    return error.getMsg();
                }
                if (!isNullOrBlank(error.getErrorDescription())) {
                    return error.getErrorDescription();
                }
            }
        } catch (Exception exception) {
            Log.e(TAG, "Failed to parse rest error", exception);
        }
        return fallbackMessage + " (Status: " + statusCode + ")";
    }

    protected <T> void postSuccess(ApiCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    protected <T> void postError(ApiCallback<T> callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }

    private boolean isNullOrBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    protected static class AuthError {
        private String message;
        private String msg;
        @SerializedName("error_description")
        private String errorDescription;

        public String getMessage() {
            return message;
        }

        public String getMsg() {
            return msg;
        }

        public String getErrorDescription() {
            return errorDescription;
        }
    }
}
