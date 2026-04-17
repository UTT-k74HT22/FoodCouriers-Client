package com.utt.foodcouriers_client.utils.websocket;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý các listener của một subscription realtime.
 *
 * <p>Một channel logic như {@code public:notifications:user_id=eq.123} có thể được nhiều
 * màn hình cùng nghe. Class này không mở WebSocket; nó chỉ giữ danh sách listener và dispatch
 * event INSERT/UPDATE/DELETE/trạng thái kết nối đến từng listener.</p>
 */
public class RealtimeChannel {
    private final String id;
    private final List<RealtimeListener> listeners = new ArrayList<>();

    /**
     * @param id id logic của channel, thường có dạng {@code schema:table:filter}
     */
    public RealtimeChannel(String id) {
        this.id = id;
    }

    /**
     * @return id logic của channel
     */
    public String getId() {
        return id;
    }

    /**
     * Thêm listener nhận event realtime.
     *
     * @param listener listener cần thêm
     */
    public void addListener(RealtimeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RealtimeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Gỡ nhiều listener cùng lúc. Dùng khi caller unsubscribe bằng channel handle riêng.
     *
     * @param listenersToRemove danh sách listener cần gỡ
     */
    public void removeListeners(List<RealtimeListener> listenersToRemove) {
        listeners.removeAll(listenersToRemove);
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    /**
     * Tạo bản sao listener để unsubscribe không làm thay đổi list đang duyệt.
     *
     * @return snapshot listener hiện tại
     */
    public List<RealtimeListener> getListenersSnapshot() {
        return new ArrayList<>(listeners);
    }

    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * Dispatch event INSERT tới mọi listener.
     *
     * @param data record mới từ Supabase
     */
    public void notifyInsert(JsonObject data) {
        for (var l : listeners) {
            l.onInsert(data);
        }
    }

    /**
     * Dispatch event UPDATE tới mọi listener.
     *
     * @param data record sau khi cập nhật
     * @param oldData record trước khi cập nhật nếu Supabase gửi kèm
     */
    public void notifyUpdate(JsonObject data, JsonObject oldData) {
        for (var l : listeners) {
            l.onUpdate(data, oldData);
        }
    }

    /**
     * Dispatch event DELETE tới mọi listener.
     *
     * @param oldData record bị xóa
     */
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
