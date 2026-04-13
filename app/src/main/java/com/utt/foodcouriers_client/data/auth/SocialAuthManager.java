package com.utt.foodcouriers_client.data.auth;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;

import java.util.LinkedHashMap;
import java.util.Map;

public class SocialAuthManager {
    public static final String CALLBACK_SCHEME = "com.utt.foodcouriers.client";
    public static final String CALLBACK_HOST = "auth";
    public static final String CALLBACK_PATH = "/callback";

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
        if (provider == SocialAuthProvider.GOOGLE) {
            callback.onError(context.getString(R.string.social_auth_stub_google));
            return;
        }
        callback.onError(context.getString(R.string.social_auth_stub_facebook));
    }

    public void handleCallback(@Nullable Uri uri, @NonNull RepositoryCallback<SocialAuthResult> callback) {
        callback.onSuccess(parseCallback(uri));
    }

    public void retryProfileBootstrap(@NonNull Context context, @NonNull RepositoryCallback<Boolean> callback) {
        callback.onError(context.getString(R.string.social_auth_stub_bootstrap));
    }

    public void refreshSession(@NonNull Context context, @NonNull RepositoryCallback<Boolean> callback) {
        callback.onError(context.getString(R.string.social_auth_stub_refresh));
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

        Map<String, String> parameters = new LinkedHashMap<>();
        appendParameters(parameters, uri.getQuery());
        appendParameters(parameters, uri.getFragment());

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
}
