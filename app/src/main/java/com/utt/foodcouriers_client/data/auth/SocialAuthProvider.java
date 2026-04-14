package com.utt.foodcouriers_client.data.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum SocialAuthProvider {
    GOOGLE("google", "Google"),
    FACEBOOK("facebook", "Facebook");

    private final String value;
    private final String displayName;

    SocialAuthProvider(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public static SocialAuthProvider fromValue(@Nullable String rawValue) {
        if (rawValue == null) {
            return null;
        }

        for (SocialAuthProvider provider : values()) {
            if (provider.value.equalsIgnoreCase(rawValue.trim())) {
                return provider;
            }
        }
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        return value;
    }
}
