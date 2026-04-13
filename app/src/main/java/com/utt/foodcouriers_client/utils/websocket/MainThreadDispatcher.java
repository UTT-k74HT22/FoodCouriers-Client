package com.utt.foodcouriers_client.utils.websocket;

import android.os.Handler;
import android.os.Looper;

/**
 * Sau này test không cần Android
 * Có thể thay bằng background thread hoặc executor service
 * để chạy các callback trên một thread khác, tránh block UI thread
 * khi có nhiều callback hoặc callback nặng.
 * Tuy nhiên, nếu callback cần cập nhật UI, thì vẫn phải dispatch lên main thread.
 */
public class MainThreadDispatcher implements CallbackDispatcher {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void dispatch(Runnable runnable) {
        handler.post(runnable);
    }
}