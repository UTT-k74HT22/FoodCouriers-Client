package com.utt.foodcouriers_client.utils.websocket;

/**
 * Sử dụng để dispatch một Runnable lên main thread.
 */
public interface CallbackDispatcher {
    /**
     * Dispatch a runnable to the main thread.
     * @param runnable The runnable to dispatch.
     */
    void dispatch(Runnable runnable);
}