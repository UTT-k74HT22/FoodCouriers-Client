package com.utt.foodcouriers_client.utils.websocket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Sử dụng để parse một RealtimeMessage.
 */
public class RealtimeMessageParser {

    public static class Event {
        public String topic;
        public String event;
        public JsonObject payload;
    }

    public static Event parse(String raw) {
        JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
        Event e = new Event();
        e.topic = obj.has("topic") ? obj.get("topic").getAsString() : "";
        e.event = obj.has("event") ? obj.get("event").getAsString() : "";
        e.payload = obj.has("payload") ? obj.getAsJsonObject("payload") : null;
        return e;
    }
}