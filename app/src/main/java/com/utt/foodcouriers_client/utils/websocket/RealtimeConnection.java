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
 * Sử dụng để kết nối đến một channel.
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

    public boolean isConnected() {
        return isConnected;
    }
}