package com.utt.foodcouriers_client.data.remote;

import android.os.Handler;
import android.os.Looper;

public abstract class BaseSupabaseClient {

    protected final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    protected <T> void postSuccess(ApiCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    protected <T> void postError(ApiCallback<T> callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }
}
