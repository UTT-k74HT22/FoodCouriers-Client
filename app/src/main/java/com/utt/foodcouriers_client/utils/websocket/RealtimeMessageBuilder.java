package com.utt.foodcouriers_client.utils.websocket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Sử dụng để xây dựng một RealtimeMessage.
 */
public class RealtimeMessageBuilder {

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

    public static String buildjoin(String topic, String schema, String table, String filter, String token, int ref) {
        return buildJoin(topic, schema, table, filter, token, ref);
    }

    public static String buildHeartbeat(int ref) {
        JsonObject msg = new JsonObject();
        msg.addProperty("topic", "phoenix");
        msg.addProperty("event", "heartbeat");
        msg.add("payload", new JsonObject());
        msg.addProperty("ref", String.valueOf(ref));
        return msg.toString();
    }

     public static String buildLeave(String topic, int ref) {
        JsonObject msg = new JsonObject();
        msg.addProperty("topic", topic);
        msg.addProperty("event", "phx_leave");
        msg.add("payload", new JsonObject());
        msg.addProperty("ref", String.valueOf(ref));
        return msg.toString();
    }
}
