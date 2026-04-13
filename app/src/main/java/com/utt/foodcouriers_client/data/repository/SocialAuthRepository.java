package com.utt.foodcouriers_client.data.repository;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.utt.foodcouriers_client.data.auth.SocialAuthManager;
import com.utt.foodcouriers_client.data.auth.SocialAuthProvider;
import com.utt.foodcouriers_client.data.auth.SocialAuthResult;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;

public class SocialAuthRepository {
    private static SocialAuthRepository instance;
    private final SocialAuthManager socialAuthManager;

    private SocialAuthRepository() {
        socialAuthManager = SocialAuthManager.getInstance();
    }

    public static synchronized SocialAuthRepository getInstance() {
        if (instance == null) {
            instance = new SocialAuthRepository();
        }
        return instance;
    }

    public void startAuth(
            @NonNull Context context,
            @NonNull SocialAuthProvider provider,
            @NonNull RepositoryCallback<Boolean> callback
    ) {
        socialAuthManager.launchProvider(context, provider, callback);
    }

    public void handleCallback(
            @Nullable Uri uri,
            @NonNull RepositoryCallback<SocialAuthResult> callback
    ) {
        socialAuthManager.handleCallback(uri, callback);
    }

    public void retryProfileBootstrap(
            @NonNull Context context,
            @NonNull RepositoryCallback<Boolean> callback
    ) {
        socialAuthManager.retryProfileBootstrap(context, callback);
    }

    public void refreshSession(
            @NonNull Context context,
            @NonNull RepositoryCallback<Boolean> callback
    ) {
        socialAuthManager.refreshSession(context, callback);
    }
}
