package com.utt.foodcouriers_client.utils.websocket;

/**
 * Abstraction quyết định callback realtime sẽ chạy trên thread nào.
 *
 * <p>Production dùng {@link MainThreadDispatcher} để callback có thể cập nhật UI an toàn.
 * Test có thể truyền dispatcher khác để chạy đồng bộ hoặc chạy trên executor riêng.</p>
 */
public interface CallbackDispatcher {
    /**
     * Dispatch a runnable to the main thread.
     * @param runnable The runnable to dispatch.
     */
    void dispatch(Runnable runnable);
}
