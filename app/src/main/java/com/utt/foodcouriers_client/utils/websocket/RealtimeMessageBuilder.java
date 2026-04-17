package com.utt.foodcouriers_client.utils.websocket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Helper tạo message JSON theo Phoenix protocol mà Supabase Realtime sử dụng.
 *
 * <p>Class này hữu ích cho test hoặc các implementation WebSocket nhỏ hơn. Client realtime
 * chính hiện build message trực tiếp trong
 * {@link com.utt.foodcouriers_client.data.remote.SupabaseRealtimeClient} để kiểm soát thêm
 * topic/filter/resubscribe.</p>
 */
public class RealtimeMessageBuilder {

    /**
     * Tạo message {@code phx_join} để subscribe postgres changes của một table.
     *
     * @param topic topic Phoenix, ví dụ {@code realtime:public:orders}
     * @param schema schema database, thường là {@code public}
     * @param table tên table cần nghe
     * @param filter filter dạng {@code column=eq.value}, hoặc null
     * @param token access token Supabase Auth để RLS áp dụng đúng user
     * @param ref mã ref tăng dần cho message
     * @return JSON string gửi qua WebSocket
     */
    public static String buildJoin(String topic, String schema, String table, String filter, String token, int ref) {
        JsonObject payload = new JsonObject(); // Sử dụng để lưu data gửi đi
        JsonObject config = new JsonObject(); // Sử dụng để lưu cấu hình
        JsonObject change = new JsonObject(); // Sử dụng để lưu thông tin thay đổi
        change.addProperty("event", "*");
        change.addProperty("schema", schema);
        change.addProperty("table", table);
        if (filter != null) {
            change.addProperty("filter", filter);
        }
        JsonArray array = new JsonArray();
        array.add(change);
        config.add("postgres_changes", array);
        if (token != null) {
            payload.addProperty("access_token", token);
        }
        payload.add("config", config);

        JsonObject msg = new JsonObject();
        msg.addProperty("topic", topic);
        msg.addProperty("event", "phx_join");
        msg.add("payload", payload);
        msg.addProperty("join_ref", String.valueOf(ref));
        msg.addProperty("ref", String.valueOf(ref));
        return msg.toString();
    }

    /**
     * Alias giữ tương thích với code cũ gọi nhầm tên method viết thường.
     *
     * @return JSON join message giống {@link #buildJoin(String, String, String, String, String, int)}
     */
    public static String buildjoin(String topic, String schema, String table, String filter, String token, int ref) {
        return buildJoin(topic, schema, table, filter, token, ref);
    }

    /**
     * Tạo heartbeat message gửi định kỳ để Phoenix không đóng socket.
     *
     * @param ref mã ref tăng dần cho message
     * @return JSON heartbeat message
     */
    public static String buildHeartbeat(int ref) {
        JsonObject msg = new JsonObject();
        msg.addProperty("topic", "phoenix");
        msg.addProperty("event", "heartbeat");
        msg.add("payload", new JsonObject());
        msg.addProperty("ref", String.valueOf(ref));
        return msg.toString();
    }

    /**
     * Tạo message {@code phx_leave} để rời một topic realtime.
     *
     * @param topic topic cần rời
     * @param ref mã ref tăng dần cho message
     * @return JSON leave message
     */
     public static String buildLeave(String topic, int ref) {
        JsonObject msg = new JsonObject();
        msg.addProperty("topic", topic);
        msg.addProperty("event", "phx_leave");
        msg.add("payload", new JsonObject());
        msg.addProperty("ref", String.valueOf(ref));
        return msg.toString();
    }
}
