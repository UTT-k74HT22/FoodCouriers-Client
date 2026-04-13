package com.utt.foodcouriers_client.data.auth;

import androidx.annotation.Nullable;

public class SocialAuthResult {
    @Nullable
    private final SocialAuthProvider provider;
    @Nullable
    private final String accessToken;
    @Nullable
    private final String refreshToken;
    @Nullable
    private final String errorCode;
    @Nullable
    private final String errorDescription;
    @Nullable
    private final String rawUri;
    private final long expiresInSeconds;

    public SocialAuthResult(
            @Nullable SocialAuthProvider provider,
            @Nullable String accessToken,
            @Nullable String refreshToken,
            @Nullable String errorCode,
            @Nullable String errorDescription,
            @Nullable String rawUri,
            long expiresInSeconds
    ) {
        this.provider = provider;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.rawUri = rawUri;
        this.expiresInSeconds = expiresInSeconds;
    }

    @Nullable
    public SocialAuthProvider getProvider() {
        return provider;
    }

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    @Nullable
    public String getRefreshToken() {
        return refreshToken;
    }

    @Nullable
    public String getErrorCode() {
        return errorCode;
    }

    @Nullable
    public String getErrorDescription() {
        return errorDescription;
    }

    @Nullable
    public String getRawUri() {
        return rawUri;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public boolean hasSessionPayload() {
        return notBlank(accessToken) && notBlank(refreshToken);
    }

    public boolean hasError() {
        return notBlank(errorCode) || notBlank(errorDescription);
    }

    public boolean isSuccessful() {
        return hasSessionPayload() && !hasError();
    }

    private boolean notBlank(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }
}
