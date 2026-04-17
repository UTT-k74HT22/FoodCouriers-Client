package com.utt.foodcouriers_client.utils.websocket;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Wrapper WebSocket đơn giản có reconnect cố định.
 *
 * <p>Lớp này chỉ chịu trách nhiệm mở socket, gửi message và gọi callback thô.
 * Luồng Supabase Realtime đầy đủ của app hiện nằm ở
 * {@link com.utt.foodcouriers_client.data.remote.SupabaseRealtimeClient}; class này vẫn hữu ích
 * nếu cần test hoặc tạo connection độc lập cho một channel.</p>
 */
public class RealtimeConnection {

    private final OkHttpClient client;
    private WebSocket socket;

    private boolean isConnected = false;
    private boolean shouldReconnect = true;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Runnable onOpen;
    private final java.util.function.Consumer<String> onMessage;
    private final Runnable onClose;
    private final java.util.function.Consumer<String> onError;

    /**
     * Tạo connection với các callback vòng đời WebSocket.
     *
     * @param onOpen chạy khi socket mở thành công
     * @param onMessage nhận message text từ socket
     * @param onClose chạy khi socket đóng
     * @param onError nhận lỗi socket
     */
    public RealtimeConnection(
            Runnable onOpen,
            java.util.function.Consumer<String> onMessage,
            Runnable onClose,
            java.util.function.Consumer<String> onError
    ) {
        this.client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();

        this.onOpen = onOpen;
        this.onMessage = onMessage;
        this.onClose = onClose;
        this.onError = onError;
    }

    /**
     * Mở WebSocket đến URL realtime.
     *
     * @param url WebSocket URL cần kết nối
     */
    public void connect(String url) {
        Request request = new Request.Builder().url(url).build();

        socket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                isConnected = true;
                onOpen.run();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                onMessage.accept(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                isConnected = false;
                onError.accept(t.getMessage());
                reconnect(url);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                isConnected = false;
                onClose.run();
                if (shouldReconnect) reconnect(url);
            }
        });
    }

    /**
     * Gửi message qua socket nếu đang connected.
     *
     * @param msg JSON/message cần gửi
     */
    public void send(String msg) {
        if (socket != null && isConnected) {
            socket.send(msg);
        }
    }

    private void reconnect(String url) {
        scheduler.schedule(() -> {
            if (shouldReconnect) {
                connect(url);
            }
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * @return {@code true} nếu socket đang mở
     */
    public boolean isConnected() {
        return isConnected;
    }
}
