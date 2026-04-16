package com.utt.foodcouriers_client.data.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.utt.foodcouriers_client.FoodCouriersClientApp;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.UserProfile;
import com.utt.foodcouriers_client.data.remote.AuthClient;
import com.utt.foodcouriers_client.data.remote.SupabaseConfig;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.SessionStore;

import java.util.LinkedHashMap;
import java.util.Map;

public class SocialAuthManager {
    public static final String TAG = "SocialAuthManager";

    public static final String CALLBACK_SCHEME = "com.utt.foodcouriers.client";
    public static final String CALLBACK_HOST = "auth";
    public static final String CALLBACK_PATH = "/callback";

    private static final String PREF_SOCIAL_AUTH = "social_auth_pref";
    private static final String KEY_PENDING_PROVIDER = "pending_provider";
    private static final long DEFAULT_TOKEN_EXPIRY_MILLIS = 3600000L;

    private static SocialAuthManager instance;

    public static synchronized SocialAuthManager getInstance() {
        if (instance == null) {
            instance = new SocialAuthManager();
        }
        return instance;
    }

    public void launchProvider(
            @NonNull Context context,
            @NonNull SocialAuthProvider provider,
            @NonNull RepositoryCallback<Boolean> callback
    ) {
        logStep(1, "Preparing Supabase OAuth launch", "provider=" + provider.getValue());

        if (provider != SocialAuthProvider.GOOGLE) {
            callback.onError(context.getString(R.string.social_auth_stub_facebook));
            return;
        }

        try {
            String redirectTo = buildRedirectUri();
            savePendingProvider(context, provider);

            String authorizeUrl = SupabaseConfig.SUPABASE_URL
                    + "/auth/v1/authorize"
                    + "?provider=" + Uri.encode(provider.getValue())
                    + "&redirect_to=" + Uri.encode(redirectTo);

            logStep(
                    2,
                    "Opening browser for Supabase OAuth",
                    "redirectTo=" + redirectTo + ", authorizeUrl=" + authorizeUrl
            );

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl));
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(browserIntent);

            callback.onSuccess(true);
        } catch (Exception e) {
            logStepError(2, "Failed to open Supabase OAuth browser flow", e);
            callback.onError("Could not open the Google sign-in flow: " + e.getMessage());
        }
    }

    public void handleCallback(
            @NonNull Context context,
            @Nullable Uri uri,
            @NonNull RepositoryCallback<SocialAuthResult> callback
    ) {
        try {
            logStep(3, "Received OAuth callback", "uri=" + (uri != null ? uri.toString() : "null"));

            SocialAuthResult result = enrichProvider(parseCallback(uri), getPendingProvider(context));

            if (!isExpectedCallbackUri(uri)) {
                clearPendingProvider(context);
                callback.onSuccess(new SocialAuthResult(
                        null,
                        null,
                        null,
                        "invalid_callback_uri",
                        "The callback deep link does not match the configured app redirect.",
                        uri != null ? uri.toString() : null,
                        0L
                ));
                return;
            }
            clearPendingProvider(context);

            if (result.isSuccessful()) {
                logStep(
                        4,
                        "Supabase callback contained session payload",
                        "provider=" + resolveProviderValue(result)
                                + ", accessToken=present, refreshToken=present"
                );
                SessionStore.saveSession(context, result);
                bootstrapSession(context, result, callback);
                return;
            }

            logStep(
                    4,
                    "Supabase callback did not complete login",
                    "provider=" + resolveProviderValue(result)
                            + ", errorCode=" + safeValue(result.getErrorCode())
                            + ", errorDescription=" + safeValue(result.getErrorDescription())
            );
            callback.onSuccess(result);
        } catch (Exception e) {
            clearPendingProvider(context);
            logStepError(4, "Failed to process Supabase OAuth callback", e);
            callback.onError("Failed to process the social auth callback: " + e.getMessage());
        }
    }

    public void retryProfileBootstrap(@NonNull Context context, @NonNull RepositoryCallback<Boolean> callback) {
        if (SessionStore.hasSession(context)) {
            bootstrapSession(context, buildStoredSessionResult(context), new RepositoryCallback<SocialAuthResult>() {
                @Override
                public void onSuccess(SocialAuthResult result) {
                    callback.onSuccess(true);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }
            });
        } else {
            callback.onError("No social auth session is available to bootstrap.");
        }
    }

    public void refreshSession(@NonNull Context context, @NonNull RepositoryCallback<Boolean> callback) {
        if (SessionStore.hasSession(context)) {
            callback.onSuccess(true);
        } else {
            callback.onError("No social auth session is available to refresh.");
        }
    }

    @NonNull
    public SocialAuthResult parseCallback(@Nullable Uri uri) {
        if (uri == null) {
            return new SocialAuthResult(
                    null,
                    null,
                    null,
                    null,
                    "No callback data was received from the browser redirect.",
                    null,
                    0L
            );
        }

        Map<String, String> parameters = extractParameters(uri);

        String rawProvider = firstNonBlank(
                parameters.get("provider"),
                parameters.get("provider_name"),
                resolveProviderFromState(parameters.get("state"))
        );

        String accessToken = parameters.get("access_token");
        String refreshToken = parameters.get("refresh_token");
        String errorCode = firstNonBlank(parameters.get("error_code"), parameters.get("error"));
        String errorDescription = firstNonBlank(
                parameters.get("error_description"),
                parameters.get("error"),
                parameters.get("message")
        );

        if (TextUtils.isEmpty(errorDescription) && TextUtils.isEmpty(accessToken) && TextUtils.isEmpty(refreshToken)) {
            errorDescription = "Callback reached the app, but no session payload was found yet.";
        }

        return new SocialAuthResult(
                SocialAuthProvider.fromValue(rawProvider),
                accessToken,
                refreshToken,
                errorCode,
                errorDescription,
                uri.toString(),
                parseLong(parameters.get("expires_in"))
        );
    }

    @NonNull
    private Map<String, String> extractParameters(@NonNull Uri uri) {
        Map<String, String> parameters = new LinkedHashMap<>();
        appendParameters(parameters, uri.getQuery());
        appendParameters(parameters, uri.getFragment());
        return parameters;
    }

    private boolean isExpectedCallbackUri(@Nullable Uri uri) {
        if (uri == null) {
            return false;
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        String path = uri.getPath();

        return CALLBACK_SCHEME.equalsIgnoreCase(scheme)
                && CALLBACK_HOST.equalsIgnoreCase(host)
                && path != null
                && path.startsWith(CALLBACK_PATH);
    }

    @NonNull
    private String buildRedirectUri() {
        return CALLBACK_SCHEME + "://" + CALLBACK_HOST + CALLBACK_PATH;
    }

    private void savePendingProvider(@NonNull Context context, @NonNull SocialAuthProvider provider) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SOCIAL_AUTH, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_PENDING_PROVIDER, provider.getValue())
                .apply();
    }

    private void clearPendingProvider(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SOCIAL_AUTH, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_PENDING_PROVIDER)
                .apply();
    }

    @Nullable
    private SocialAuthProvider getPendingProvider(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SOCIAL_AUTH, Context.MODE_PRIVATE);
        return SocialAuthProvider.fromValue(prefs.getString(KEY_PENDING_PROVIDER, null));
    }

    private void bootstrapSession(
            @NonNull Context context,
            @NonNull SocialAuthResult result,
            @NonNull RepositoryCallback<SocialAuthResult> callback
    ) {
        AuthClient.getInstance().bootstrapSocialSession(
                result.getAccessToken(),
                result.getRefreshToken(),
                new AuthClient.ApiCallback<UserProfile>() {
                    @Override
                    public void onSuccess(UserProfile userProfile) {
                        if (!userProfile.isActive()) {
                            SessionStore.clearSession(context);
                            SessionManager.getInstance(context).clearSession();
                            AuthClient.getInstance().clearSession();
                            logStep(6, "Bootstrap rejected disabled account", "provider=" + resolveProviderValue(result));
                            callback.onError(context.getString(R.string.error_account_disabled));
                            return;
                        }

                        SessionManager.getInstance(context).saveSession(
                                result.getAccessToken(),
                                result.getRefreshToken(),
                                userProfile,
                                resolveExpiryMillis(result)
                        );
                        FoodCouriersClientApp.initializeRealtime(result.getAccessToken());
                        logStep(
                                6,
                                "Bootstrap completed and session saved",
                                "provider=" + resolveProviderValue(result)
                                        + ", userId=" + safeValue(userProfile.getId())
                                        + ", email=" + safeValue(userProfile.getEmail())
                        );
                        callback.onSuccess(result);
                    }

                    @Override
                    public void onError(String error) {
                        SessionManager.getInstance(context).clearSession();
                        AuthClient.getInstance().clearSession();
                        logStep(6, "Bootstrap failed while loading profile", "error=" + safeValue(error));
                        callback.onError(error);
                    }
                }
        );
    }

    @NonNull
    private SocialAuthResult buildStoredSessionResult(@NonNull Context context) {
        return new SocialAuthResult(
                SocialAuthProvider.fromValue(SessionStore.getProvider(context)),
                SessionStore.getAccessToken(context),
                SessionStore.getRefreshToken(context),
                null,
                null,
                SessionStore.getRawUri(context),
                SessionStore.getExpiresInSeconds(context)
        );
    }

    private long resolveExpiryMillis(@NonNull SocialAuthResult result) {
        if (result.getExpiresInSeconds() > 0) {
            return result.getExpiresInSeconds() * 1000L;
        }
        return DEFAULT_TOKEN_EXPIRY_MILLIS;
    }

    private void appendParameters(@NonNull Map<String, String> parameters, @Nullable String encodedSegment) {
        if (TextUtils.isEmpty(encodedSegment)) {
            return;
        }

        String[] pairs = encodedSegment.split("&");
        for (String pair : pairs) {
            if (TextUtils.isEmpty(pair)) {
                continue;
            }

            String[] parts = pair.split("=", 2);
            String key = Uri.decode(parts[0]);
            String value = parts.length > 1 ? Uri.decode(parts[1]) : "";
            parameters.put(key, value);
        }
    }

    @Nullable
    private String resolveProviderFromState(@Nullable String state) {
        if (TextUtils.isEmpty(state)) {
            return null;
        }

        String normalized = state.toLowerCase();
        if (normalized.contains(SocialAuthProvider.GOOGLE.getValue())) {
            return SocialAuthProvider.GOOGLE.getValue();
        }
        if (normalized.contains(SocialAuthProvider.FACEBOOK.getValue())) {
            return SocialAuthProvider.FACEBOOK.getValue();
        }
        return null;
    }

    @NonNull
    private SocialAuthResult enrichProvider(
            @NonNull SocialAuthResult result,
            @Nullable SocialAuthProvider fallbackProvider
    ) {
        if (result.getProvider() != null || fallbackProvider == null) {
            return result;
        }

        return new SocialAuthResult(
                fallbackProvider,
                result.getAccessToken(),
                result.getRefreshToken(),
                result.getErrorCode(),
                result.getErrorDescription(),
                result.getRawUri(),
                result.getExpiresInSeconds()
        );
    }

    @Nullable
    private String firstNonBlank(@Nullable String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    private long parseLong(@Nullable String rawValue) {
        if (TextUtils.isEmpty(rawValue)) {
            return 0L;
        }
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private boolean notBlank(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }

    @NonNull
    private String resolveProviderValue(@NonNull SocialAuthResult result) {
        return result.getProvider() != null ? result.getProvider().getValue() : "unknown";
    }

    @NonNull
    private String safeValue(@Nullable String value) {
        return notBlank(value) ? value : "n/a";
    }

    private void logStep(int step, @NonNull String description, @Nullable String details) {
        String message = "Step " + step + ": " + description;
        if (notBlank(details)) {
            message += " | " + details;
        }
        Log.d(TAG, message);
    }

    private void logStepError(int step, @NonNull String description, @NonNull Throwable throwable) {
        Log.e(TAG, "Step " + step + ": " + description + " | error=" + throwable.getMessage(), throwable);
    }
}
