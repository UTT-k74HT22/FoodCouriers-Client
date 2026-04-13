package com.utt.foodcouriers_client.utils.websocket;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * quản lý listeners của một channel
 * 1 channel → nhiều listener
 * Không phụ thuộc WebSocketClient, chỉ quản lý listeners và dispatch sự kiện đến listeners
 */
public class RealtimeChannel {
    private final String id;
    private final List<RealtimeListener> listeners = new ArrayList<>();

    public RealtimeChannel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void addListener(RealtimeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RealtimeListener listener) {
        listeners.remove(listener);
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    public int getListenerCount() {
        return listeners.size();
    }

    public void notifyInsert(JsonObject data) {
        for (var l : listeners) {
            l.onInsert(data);
        }
    }

    public void notifyUpdate(JsonObject data, JsonObject oldData) {
        for (var l : listeners) {
            l.onUpdate(data, oldData);
        }
    }

    public void notifyDelete(JsonObject oldData) {
        for (var l : listeners) {
            l.onDelete(oldData);
        }
    }

    public void notifyConnected() {
        for (var l : listeners) {
            l.onConnected();
        }
    }

    public void notifyDisconnected() {
        for (var l : listeners) {
            l.onDisconnected();
        }
    }

    public void notifyError(String error) {
        for (var l : listeners) {
            l.onError(error);
        }
    }
}
