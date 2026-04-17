package com.utt.foodcouriers_client.utils.websocket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Helper parse message JSON thô nhận từ Supabase Realtime/Phoenix WebSocket.
 */
public class RealtimeMessageParser {

    /**
     * Cấu trúc tối thiểu của một message Phoenix.
     */
    public static class Event {
        /** Topic Phoenix, ví dụ {@code realtime:public:orders}. */
        public String topic;
        /** Tên event, ví dụ {@code phx_reply}, {@code postgres_changes}. */
        public String event;
        /** Payload event đã parse sang JsonObject. */
        public JsonObject payload;
    }

    /**
     * Parse chuỗi JSON thành event đơn giản.
     *
     * @param raw JSON string nhận từ WebSocket
     * @return event chứa topic/event/payload
     */
    public static Event parse(String raw) {
        JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
        Event e = new Event();
        e.topic = obj.has("topic") ? obj.get("topic").getAsString() : "";
        e.event = obj.has("event") ? obj.get("event").getAsString() : "";
        e.payload = obj.has("payload") ? obj.getAsJsonObject("payload") : null;
        return e;
    }
}
