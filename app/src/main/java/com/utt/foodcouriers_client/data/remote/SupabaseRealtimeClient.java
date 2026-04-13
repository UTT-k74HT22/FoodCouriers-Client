package com.utt.foodcouriers_client.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.utt.foodcouriers_client.utils.websocket.CallbackDispatcher;
import com.utt.foodcouriers_client.utils.websocket.MainThreadDispatcher;
import com.utt.foodcouriers_client.utils.websocket.RealtimeChannel;
import com.utt.foodcouriers_client.utils.websocket.RealtimeListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Supabase Realtime Client - Singleton.
 * 
 * Quản lý kết nối WebSocket đến Supabase Realtime server.
 * Hỗ trợ:
 * - Kết nối/ngắt kết nối WebSocket
 * - Subscribe/unsubscribe channel để lắng nghe thay đổi database
 * - Heartbeat để duy trì kết nối (Phoenix protocol yêu cầu heartbeat mỗi 30s)
 * - Auto-reconnect khi mất kết nối
 * - Dispatch events về các RealtimeListener trên main thread
 * 
 * Cách sử dụng:
 * <pre>
 * SupabaseRealtimeClient client = SupabaseRealtimeClient.getInstance();
 * client.connect();
 * 
 * RealtimeChannel channel = client.subscribe("public:orders", "id=eq." + orderId, new RealtimeListener() {
 *     {@literal @}Override
 *     public void onInsert(JsonObject record) { ... }
 *     {@literal @}Override
 *     public void onUpdate(JsonObject record, JsonObject oldRecord) { ... }
 *     {@literal @}Override
 *     public void onDelete(JsonObject oldRecord) { ... }
 * });
 * </pre>
 */
public class SupabaseRealtimeClient {

    private static final String TAG = "SupabaseRealtime";
    private static final String REALTIME_VSN = "1.0.0";
    
    /** Heartbeat interval: 25s (Phoenix timeout là 30s, gửi sớm 5s để an toàn) */
    private static final int HEARTBEAT_INTERVAL_SECONDS = 25;
    
    /** Delay trước khi reconnect sau khi mất kết nối */
    private static final int RECONNECT_DELAY_SECONDS = 5;
    
    /** Số lần retry tối đa trước khi dừng reconnect */
    private static final int MAX_RECONNECT_ATTEMPTS = 10;

    private static SupabaseRealtimeClient instance;

    private WebSocket webSocket;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Handler mainHandler;
    private final AtomicInteger refCounter = new AtomicInteger(0);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    
    /** Map channelId -> RealtimeChannel (quản lý listeners) */
    private final Map<String, RealtimeChannel> channels = new HashMap<>();
    
    /** Map channelId -> topic (để unsubscribe) */
    private final Map<String, String> channelTopics = new HashMap<>();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> heartbeatFuture;
    private boolean isConnected = false;
    private boolean shouldReconnect = true;
    private String accessToken;
    private String apikey;
    private CallbackDispatcher callbackDispatcher;

    private SupabaseRealtimeClient() {
        httpClient = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
        scheduler = Executors.newSingleThreadScheduledExecutor();
        callbackDispatcher = new MainThreadDispatcher();
    }

    /**
     * Lấy instance singleton của SupabaseRealtimeClient.
     * 
     * @return instance duy nhất của client
     */
    public static synchronized SupabaseRealtimeClient getInstance() {
        if (instance == null) {
            instance = new SupabaseRealtimeClient();
        }
        return instance;
    }

    /**
     * Khởi tạo client với access token.
     * Nên gọi sau khi user đăng nhập thành công.
     * 
     * @param accessToken JWT token từ Supabase Auth
     */
    public void initialize(String accessToken) {
        this.accessToken = accessToken;
        this.apikey = SupabaseConfig.SUPABASE_ANON_KEY;
    }

    /**
     * Thiết lập callback dispatcher tùy chỉnh.
     * Mặc định sử dụng MainThreadDispatcher để dispatch về main thread.
     * 
     * @param dispatcher callback dispatcher tùy chỉnh
     */
    public void setCallbackDispatcher(CallbackDispatcher dispatcher) {
        this.callbackDispatcher = dispatcher;
    }

    /**
     * Lấy URL WebSocket cho Supabase Realtime.
     * 
     * @return URL WebSocket đầy đủ với apikey và vsn
     */
    public String getRealtimeWebsocketUrl() {
        return SupabaseConfig.getRealtimeWebsocketUrl();
    }

    /**
     * Thiết lập access token.
     * Nên gọi sau khi refresh token.
     * 
     * @param accessToken JWT token mới
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Kết nối đến Supabase Realtime server.
     * Nếu đã connected, không làm gì.
     */
    public void connect() {
        if (isConnected) {
            Log.d(TAG, "Already connected");
            return;
        }

        String url = getRealtimeWebsocketUrl();
        if (url.isEmpty()) {
            Log.e(TAG, "Realtime URL not configured");
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", apikey != null ? apikey : "")
                .build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket connected");
                isConnected = true;
                reconnectAttempts.set(0);
                startHeartbeat();
                resubscribeAllChannels();
                notifyChannelsConnected();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleMessage(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket failure: " + t.getMessage());
                isConnected = false;
                stopHeartbeat();
                notifyChannelsError("Connection failed: " + t.getMessage());
                if (shouldReconnect) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + code + " - " + reason);
                isConnected = false;
                stopHeartbeat();
                notifyChannelsDisconnected();
                if (shouldReconnect && code != 1000) {
                    scheduleReconnect();
                }
            }
        });
    }

    /**
     * Ngắt kết nối WebSocket.
     * Đặt shouldReconnect = false để không tự động reconnect.
     */
    public void disconnect() {
        shouldReconnect = false;
        isConnected = false;
        stopHeartbeat();
        
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
            webSocket = null;
        }
        
        channels.clear();
        channelTopics.clear();
    }

    /**
     * Subscribe vào một database table để lắng nghe thay đổi.
     * 
     * @param table Tên table (vd: "orders", "products")
     * @param schema Schema của table (vd: "public")
     * @param filter Filter query (vd: "restaurant_id=eq.123"). Để null nếu muốn lắng nghe tất cả.
     * @param listener Listener để nhận events
     * @return RealtimeChannel - có thể dùng để unsubscribe
     */
    public RealtimeChannel subscribe(String table, String schema, String filter, RealtimeListener listener) {
        String channelId = schema + ":" + table + (filter != null ? ":" + filter : "");
        
        RealtimeChannel channel;
        if (channels.containsKey(channelId)) {
            channel = channels.get(channelId);
            channel.addListener(listener);
        } else {
            channel = new RealtimeChannel(channelId);
            channel.addListener(listener);
            channels.put(channelId, channel);
            
            if (isConnected) {
                String topic = "realtime:" + schema + ":" + table;
                channelTopics.put(channelId, topic);
                sendJoin(topic, schema, table, filter);
            }
        }
        
        return channel;
    }

    /**
     * Subscribe vào một channel với shorthand.
     * 
     * @param channelId ID của channel (vd: "public:orders")
     * @param filter Filter query (vd: "id=eq.123")
     * @param listener Listener để nhận events
     * @return RealtimeChannel
     */
    public RealtimeChannel subscribe(String channelId, String filter, RealtimeListener listener) {
        String[] parts = channelId.split(":", 2);
        if (parts.length < 2) {
            Log.e(TAG, "Invalid channel ID format. Use 'schema:table'");
            return null;
        }
        return subscribe(parts[1], parts[0], filter, listener);
    }

    /**
     * Unsubscribe khỏi một channel.
     * 
     * @param channel RealtimeChannel đã được trả về từ subscribe()
     */
    public void unsubscribe(RealtimeChannel channel) {
        if (channel == null) return;
        
        String channelId = channel.getId();
        RealtimeChannel existingChannel = channels.get(channelId);
        
        if (existingChannel != null) {
            existingChannel.removeAllListeners();
            channels.remove(channelId);
            
            String topic = channelTopics.remove(channelId);
            if (topic != null && isConnected) {
                sendLeave(topic);
            }
        }
    }

    /**
     * Unsubscribe bằng channel ID.
     * 
     * @param channelId ID của channel
     */
    public void unsubscribeById(String channelId) {
        RealtimeChannel channel = channels.get(channelId);
        if (channel != null) {
            unsubscribe(channel);
        }
    }

    /**
     * Kiểm tra xem WebSocket đã connected chưa.
     * 
     * @return true nếu đang connected
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Kiểm tra xem đang trong trạng thái reconnect hay không.
     * 
     * @return true nếu đang schedule reconnect
     */
    public boolean isReconnecting() {
        return shouldReconnect && !isConnected;
    }

    private void startHeartbeat() {
        stopHeartbeat();
        
        heartbeatFuture = scheduler.scheduleAtFixedRate(() -> {
            if (isConnected && webSocket != null) {
                int ref = refCounter.incrementAndGet();
                JsonObject heartbeat = new JsonObject();
                heartbeat.addProperty("topic", "phoenix");
                heartbeat.addProperty("event", "heartbeat");
                heartbeat.add("payload", new JsonObject());
                heartbeat.addProperty("ref", String.valueOf(ref));
                
                webSocket.send(heartbeat.toString());
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
            heartbeatFuture = null;
        }
    }

    private void scheduleReconnect() {
        int attempts = reconnectAttempts.incrementAndGet();
        if (attempts > MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnect attempts reached. Stop reconnecting.");
            shouldReconnect = false;
            notifyChannelsError("Max reconnect attempts reached");
            return;
        }

        Log.d(TAG, "Scheduling reconnect attempt " + attempts + " in " + RECONNECT_DELAY_SECONDS + "s");
        
        scheduler.schedule(() -> {
            if (shouldReconnect && !isConnected) {
                connect();
            }
        }, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private void sendJoin(String topic, String schema, String table, String filter) {
        int ref = refCounter.incrementAndGet();
        
        JsonObject payload = new JsonObject();
        JsonObject config = new JsonObject();
        JsonObject change = new JsonObject();
        
        change.addProperty("event", "*");
        change.addProperty("schema", schema);
        change.addProperty("table", table);
        if (filter != null && !filter.isEmpty()) {
            change.addProperty("filter", filter);
        }
        
        JsonArray changes = new JsonArray();
        changes.add(change);
        config.add("postgres_changes", changes);
        
        if (accessToken != null && !accessToken.isEmpty()) {
            payload.addProperty("access_token", accessToken);
        }
        payload.add("config", config);
        
        JsonObject msg = new JsonObject();
        msg.addProperty("topic", topic);
        msg.addProperty("event", "phx_join");
        msg.add("payload", payload);
        msg.addProperty("join_ref", String.valueOf(ref));
        msg.addProperty("ref", String.valueOf(ref));
        
        if (webSocket != null) {
            webSocket.send(msg.toString());
        }
    }

    private void sendLeave(String topic) {
        int ref = refCounter.incrementAndGet();
        
        JsonObject msg = new JsonObject();
        msg.addProperty("topic", topic);
        msg.addProperty("event", "phx_leave");
        msg.add("payload", new JsonObject());
        msg.addProperty("ref", String.valueOf(ref));
        
        if (webSocket != null) {
            webSocket.send(msg.toString());
        }
    }

    private void resubscribeAllChannels() {
        for (Map.Entry<String, RealtimeChannel> entry : channels.entrySet()) {
            String channelId = entry.getKey();
            String[] parts = channelId.split(":", 3);
            
            if (parts.length >= 2) {
                String schema = parts[0];
                String table = parts[1];
                String filter = parts.length > 2 ? parts[2] : null;
                
                String topic = "realtime:" + schema + ":" + table;
                channelTopics.put(channelId, topic);
                sendJoin(topic, schema, table, filter);
            }
        }
    }

    private void handleMessage(String rawMessage) {
        try {
            JsonObject obj = JsonParser.parseString(rawMessage).getAsJsonObject();
            
            String topic = obj.has("topic") ? obj.get("topic").getAsString() : "";
            String event = obj.has("event") ? obj.get("event").getAsString() : "";
            JsonObject payload = obj.has("payload") ? obj.getAsJsonObject("payload") : null;
            
            if (payload == null) return;
            
            switch (event) {
                case "phx_reply":
                    handlePhxReply(payload);
                    break;
                    
                case "postgres_changes":
                    handlePostgresChanges(payload, topic);
                    break;
                    
                default:
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing message: " + e.getMessage());
        }
    }

    private void handlePhxReply(JsonObject payload) {
        String status = payload.has("status") ? payload.get("status").getAsString() : "";
        
        if ("ok".equals(status)) {
            Log.d(TAG, "Phoenix reply: ok");
        } else if ("error".equals(status)) {
            String error = payload.has("response") ? 
                payload.getAsJsonObject("response").toString() : "Unknown error";
            Log.e(TAG, "Phoenix reply error: " + error);
            notifyChannelsError(error);
        }
    }

    private void handlePostgresChanges(JsonObject payload, String topic) {
        JsonObject data = payload.has("data") ? payload.getAsJsonObject("data") : null;
        if (data == null) return;
        
        String commitType = data.has("commit_type") ? data.get("commit_type").getAsString() : "";
        JsonArray changes = data.has("changes") ? data.getAsJsonArray("changes") : null;
        
        if (changes == null || changes.size() == 0) return;
        
        String schema = extractSchemaFromTopic(topic);
        String table = extractTableFromTopic(topic);
        String channelId = schema + ":" + table;
        
        RealtimeChannel channel = channels.get(channelId);
        if (channel == null) return;
        
        for (JsonElement changeElem : changes) {
            JsonObject change = changeElem.getAsJsonObject();
            JsonObject newRecord = change.has("new") ? change.getAsJsonObject("new") : null;
            JsonObject oldRecord = change.has("old") ? change.getAsJsonObject("old") : null;
            
            switch (commitType) {
                case "insert":
                    if (newRecord != null) {
                        notifyChannel(channel, c -> c.notifyInsert(newRecord));
                    }
                    break;
                    
                case "update":
                    if (newRecord != null) {
                        notifyChannel(channel, c -> c.notifyUpdate(newRecord, oldRecord));
                    }
                    break;
                    
                case "delete":
                    if (oldRecord != null) {
                        notifyChannel(channel, c -> c.notifyDelete(oldRecord));
                    }
                    break;
                    
                default:
                    break;
            }
        }
    }

    private String extractSchemaFromTopic(String topic) {
        String[] parts = topic.split(":");
        return parts.length >= 2 ? parts[1] : "public";
    }

    private String extractTableFromTopic(String topic) {
        String[] parts = topic.split(":");
        return parts.length >= 3 ? parts[2] : "";
    }

    private void notifyChannel(RealtimeChannel channel, java.util.function.Consumer<RealtimeChannel> action) {
        if (callbackDispatcher != null) {
            callbackDispatcher.dispatch(() -> action.accept(channel));
        } else {
            action.accept(channel);
        }
    }

    private void notifyChannelsConnected() {
        for (RealtimeChannel channel : channels.values()) {
            notifyChannel(channel, RealtimeChannel::notifyConnected);
        }
    }

    private void notifyChannelsDisconnected() {
        for (RealtimeChannel channel : channels.values()) {
            notifyChannel(channel, RealtimeChannel::notifyDisconnected);
        }
    }

    private void notifyChannelsError(String error) {
        for (RealtimeChannel channel : channels.values()) {
            notifyChannel(channel, c -> c.notifyError(error));
        }
    }
}