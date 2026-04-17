package com.utt.foodcouriers_client.data.remote;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
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
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
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
    
    /** Delay khởi điểm trước khi reconnect sau khi mất kết nối */
    private static final int INITIAL_RECONNECT_DELAY_SECONDS = 5;

    /** Delay reconnect tối đa để tránh spam log/network khi DNS hoặc internet lỗi */
    private static final int MAX_RECONNECT_DELAY_SECONDS = 60;
    
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
    private Context appContext;

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
        this.shouldReconnect = true;
    }

    /**
     * Khởi tạo client với application context để kiểm tra trạng thái mạng trước khi reconnect.
     *
     * @param context Application/Activity context
     * @param accessToken JWT token từ Supabase Auth
     */
    public void initialize(Context context, String accessToken) {
        this.appContext = context != null ? context.getApplicationContext() : null;
        initialize(accessToken);
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
        if (isConnected && webSocket != null) {
            sendAccessToken();
        }
    }

    /**
     * Kết nối đến Supabase Realtime server.
     *
     * <p>Nếu đã connected thì bỏ qua. Nếu chưa có mạng hợp lệ, client không mở socket ngay
     * mà báo lỗi cho các channel hiện có và schedule reconnect. Khi socket mở thành công,
     * client bắt đầu heartbeat, join lại tất cả channel đã đăng ký và báo {@code onConnected}
     * cho listener.</p>
     */
    public void connect() {
        if (isConnected) {
            Log.d(TAG, "Already connected");
            return;
        }
        shouldReconnect = true;

        if (!hasUsableNetwork()) {
            Log.w(TAG, "Network unavailable. Realtime reconnect will be retried later.");
            notifyChannelsError("Network unavailable");
            scheduleReconnect();
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
                String failureMessage = buildFailureMessage(t, response);
                if (isTransientSocketAbort(t) || !hasUsableNetwork()) {
                    Log.w(TAG, "WebSocket disconnected: " + failureMessage);
                } else {
                    Log.e(TAG, "WebSocket failure: " + failureMessage);
                }
                isConnected = false;
                stopHeartbeat();
                notifyChannelsError("Connection failed: " + buildUserFacingFailureMessage(t));
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
     * <p>Nhiều nơi trong app có thể subscribe cùng một channel id. Client gom các listener vào
     * một {@link RealtimeChannel} thật để chỉ join Supabase một lần, nhưng trả về một
     * {@link RealtimeChannel} riêng cho caller. Khi caller unsubscribe, chỉ listener của caller
     * bị gỡ; socket chỉ gửi {@code phx_leave} khi không còn listener nào.</p>
     *
     * @param table tên table, ví dụ {@code orders} hoặc {@code notifications}
     * @param schema schema của table, thường là {@code public}
     * @param filter filter Supabase Realtime, ví dụ {@code user_id=eq.123}; null để nghe tất cả
     * @param listener listener nhận INSERT/UPDATE/DELETE và trạng thái kết nối
     * @return channel handle để caller truyền lại vào {@link #unsubscribe(RealtimeChannel)}
     */
    public RealtimeChannel subscribe(String table, String schema, String filter, RealtimeListener listener) {
        String channelId = schema + ":" + table + (filter != null ? ":" + filter : "");
        RealtimeChannel subscription = new RealtimeChannel(channelId);
        subscription.addListener(listener);
        
        RealtimeChannel channel;
        if (channels.containsKey(channelId)) {
            channel = channels.get(channelId);
            channel.addListener(listener);
        } else {
            channel = new RealtimeChannel(channelId);
            channel.addListener(listener);
            channels.put(channelId, channel);
            
            if (isConnected) {
                String topic = buildTopic(channelId, schema, table, filter);
                channelTopics.put(channelId, topic);
                sendJoin(topic, schema, table, filter);
            }
        }
        
        return subscription;
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
            existingChannel.removeListeners(channel.getListenersSnapshot());

            if (existingChannel.getListenerCount() == 0) {
                channels.remove(channelId);

                String topic = channelTopics.remove(channelId);
                if (topic != null && isConnected) {
                    sendLeave(topic);
                }
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

    /**
     * Gửi heartbeat Phoenix định kỳ để giữ WebSocket sống.
     */
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

    /**
     * Lên lịch reconnect với backoff tăng dần khi socket mất kết nối.
     */
    private void scheduleReconnect() {
        int attempts = reconnectAttempts.incrementAndGet();
        if (attempts > MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnect attempts reached. Stop reconnecting.");
            shouldReconnect = false;
            notifyChannelsError("Max reconnect attempts reached");
            return;
        }

        int delaySeconds = getReconnectDelaySeconds(attempts);
        Log.d(TAG, "Scheduling reconnect attempt " + attempts + " in " + delaySeconds + "s");
        
        scheduler.schedule(() -> {
            if (shouldReconnect && !isConnected) {
                connect();
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private int getReconnectDelaySeconds(int attempts) {
        long delay = INITIAL_RECONNECT_DELAY_SECONDS * (1L << Math.min(attempts - 1, 4));
        return (int) Math.min(delay, MAX_RECONNECT_DELAY_SECONDS);
    }

    private boolean hasUsableNetwork() {
        if (appContext == null) {
            return true;
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return true;
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) {
            return false;
        }

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private String buildFailureMessage(Throwable t, Response response) {
        StringBuilder message = new StringBuilder();
        if (t != null) {
            message.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage());
        } else {
            message.append("unknown error");
        }
        if (response != null) {
            message.append(", httpCode=").append(response.code());
        }
        return message.toString();
    }

    private String buildUserFacingFailureMessage(Throwable t) {
        if (t instanceof UnknownHostException) {
            return "Cannot resolve Supabase host. Check internet, DNS, VPN, or emulator network.";
        }
        if (isTransientSocketAbort(t) || !hasUsableNetwork()) {
            return "Realtime connection temporarily unavailable. Reconnecting...";
        }
        return t != null && t.getMessage() != null ? t.getMessage() : "Realtime connection failed";
    }

    private boolean isTransientSocketAbort(Throwable t) {
        return t instanceof SocketException
                && t.getMessage() != null
                && t.getMessage().toLowerCase().contains("software caused connection abort");
    }

    /**
     * Gửi message {@code phx_join} để đăng ký postgres_changes cho một table/filter.
     */
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
            Log.d(TAG, "Joining realtime topic=" + topic + ", table=" + schema + "." + table + ", filter=" + filter);
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

    /**
     * Gửi access token mới tới các topic đang join sau khi token được refresh.
     */
    private void sendAccessToken() {
        for (String topic : new java.util.HashSet<>(channelTopics.values())) {
            int ref = refCounter.incrementAndGet();

            JsonObject payload = new JsonObject();
            payload.addProperty("access_token", accessToken);

            JsonObject msg = new JsonObject();
            msg.addProperty("topic", topic);
            msg.addProperty("event", "access_token");
            msg.add("payload", payload);
            msg.addProperty("join_ref", String.valueOf(ref));
            msg.addProperty("ref", String.valueOf(ref));

            webSocket.send(msg.toString());
        }
    }

    /**
     * Join lại toàn bộ channel sau khi reconnect.
     */
    private void resubscribeAllChannels() {
        for (Map.Entry<String, RealtimeChannel> entry : channels.entrySet()) {
            String channelId = entry.getKey();
            String[] parts = channelId.split(":", 3);
            
            if (parts.length >= 2) {
                String schema = parts[0];
                String table = parts[1];
                String filter = parts.length > 2 ? parts[2] : null;
                
                String topic = buildTopic(channelId, schema, table, filter);
                channelTopics.put(channelId, topic);
                sendJoin(topic, schema, table, filter);
            }
        }
    }

    private String buildTopic(String channelId, String schema, String table, String filter) {
        if (filter == null || filter.isEmpty()) {
            return "realtime:" + schema + ":" + table;
        }

        String encodedFilter = Base64.encodeToString(
                filter.getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING
        );
        return "realtime:" + schema + ":" + table + ":" + encodedFilter;
    }

    /**
     * Parse message thô từ WebSocket và chuyển tới handler theo event Phoenix/Supabase.
     *
     * @param rawMessage chuỗi JSON nhận từ Supabase Realtime
     */
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
                    Log.d(TAG, "Postgres change received on topic=" + topic);
                    handlePostgresChanges(payload, topic);
                    break;

                case "system":
                    Log.d(TAG, "Realtime system message on topic=" + topic + ": " + payload);
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

    /**
     * Xử lý payload {@code postgres_changes} mới của Supabase Realtime.
     */
    private void handlePostgresChanges(JsonObject payload, String topic) {
        JsonObject data = payload.has("data") ? payload.getAsJsonObject("data") : null;
        if (data == null) return;

        if (data.has("changes") && data.get("changes").isJsonArray()) {
            handleLegacyPostgresChanges(data, topic);
            return;
        }

        String schema = getStringOrDefault(data, "schema", extractSchemaFromTopic(topic));
        String table = getStringOrDefault(data, "table", extractTableFromTopic(topic));
        String type = getStringOrDefault(data, "type", "").toLowerCase();
        JsonObject newRecord = data.has("record") && data.get("record").isJsonObject()
                ? data.getAsJsonObject("record")
                : null;
        JsonObject oldRecord = data.has("old_record") && data.get("old_record").isJsonObject()
                ? data.getAsJsonObject("old_record")
                : null;

        dispatchDatabaseChange(schema, table, type, newRecord, oldRecord);
    }

    /**
     * Xử lý payload realtime dạng cũ có mảng {@code changes}.
     */
    private void handleLegacyPostgresChanges(JsonObject data, String topic) {
        String commitType = getStringOrDefault(data, "commit_type", "").toLowerCase();
        JsonArray changes = data.getAsJsonArray("changes");
        if (changes.size() == 0) return;

        String schema = extractSchemaFromTopic(topic);
        String table = extractTableFromTopic(topic);

        for (JsonElement changeElem : changes) {
            JsonObject change = changeElem.getAsJsonObject();
            JsonObject newRecord = change.has("new") && change.get("new").isJsonObject()
                    ? change.getAsJsonObject("new")
                    : null;
            JsonObject oldRecord = change.has("old") && change.get("old").isJsonObject()
                    ? change.getAsJsonObject("old")
                    : null;

            dispatchDatabaseChange(schema, table, commitType, newRecord, oldRecord);
        }
    }

    /**
     * Dispatch INSERT/UPDATE/DELETE tới những channel trùng schema/table/filter.
     */
    private void dispatchDatabaseChange(
            String schema,
            String table,
            String type,
            JsonObject newRecord,
            JsonObject oldRecord
    ) {
        for (Map.Entry<String, RealtimeChannel> entry : channels.entrySet()) {
            String channelId = entry.getKey();
            if (!isMatchingChannel(channelId, schema, table, newRecord, oldRecord)) {
                continue;
            }

            RealtimeChannel channel = entry.getValue();
            switch (type) {
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

    /**
     * Kiểm tra event realtime có thuộc channel đã subscribe hay không.
     */
    private boolean isMatchingChannel(String channelId, String schema, String table, JsonObject newRecord, JsonObject oldRecord) {
        String[] parts = channelId.split(":", 3);
        if (parts.length < 2) {
            return false;
        }

        boolean sameTable = parts[0].equals(schema) && parts[1].equals(table);
        if (!sameTable) {
            return false;
        }

        if (parts.length < 3 || parts[2] == null || parts[2].isEmpty()) {
            return true;
        }

        JsonObject record = newRecord != null ? newRecord : oldRecord;
        return matchesFilter(record, parts[2]);
    }

    /**
     * Áp dụng filter đơn giản dạng {@code column=eq.value} ở phía client trước khi dispatch.
     */
    private boolean matchesFilter(JsonObject record, String filter) {
        if (record == null || filter == null || filter.isEmpty()) {
            return true;
        }

        int operatorIndex = filter.indexOf("=eq.");
        if (operatorIndex <= 0) {
            return true;
        }

        String column = filter.substring(0, operatorIndex);
        String expectedValue = filter.substring(operatorIndex + 4);
        if (!record.has(column) || record.get(column).isJsonNull()) {
            return false;
        }

        return expectedValue.equals(record.get(column).getAsString());
    }

    private String getStringOrDefault(JsonObject object, String key, String fallback) {
        if (object.has(key) && !object.get(key).isJsonNull()) {
            return object.get(key).getAsString();
        }
        return fallback;
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
