package com.utt.foodcouriers_client.utils.websocket;

import android.os.Handler;
import android.os.Looper;

/**
 * Dispatcher mặc định đưa callback realtime về Android main thread.
 *
 * <p>Các listener realtime thường gọi ViewModel, Toast hoặc UI state. Vì WebSocket callback
 * của OkHttp chạy ở background thread, dispatcher này dùng {@link Handler} để post về
 * {@link Looper#getMainLooper()} trước khi gọi listener.</p>
 */
public class MainThreadDispatcher implements CallbackDispatcher {

    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Post runnable lên main thread.
     *
     * @param runnable callback cần chạy trên UI thread
     */
    @Override
    public void dispatch(Runnable runnable) {
        handler.post(runnable);
    }
}
