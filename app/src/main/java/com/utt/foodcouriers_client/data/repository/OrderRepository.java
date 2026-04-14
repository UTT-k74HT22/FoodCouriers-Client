package com.utt.foodcouriers_client.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.OrderLineItem;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.data.model.PromotionValidationResult;
import com.utt.foodcouriers_client.data.remote.SupabaseConfig;
import com.utt.foodcouriers_client.utils.SessionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OrderRepository {

    private static final MediaType JSON = MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON);
    private static OrderRepository instance;

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public enum OrderFilter {
        ALL,
        ACTIVE,
        COMPLETED,
        CANCELLED
    }

    public static synchronized OrderRepository getInstance() {
        if (instance == null) {
            instance = new OrderRepository();
        }
        return instance;
    }

    public void getOrders(Context context, OrderFilter filter, RepositoryCallback<List<OrderSummary>> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }

        String userId = sessionManager.getUserId();
        String filterQuery = "";
        if (filter == OrderFilter.ACTIVE) {
            filterQuery = "&status=in.(pending,confirmed,preparing,ready_for_pickup,delivering)";
        } else if (filter == OrderFilter.COMPLETED) {
            filterQuery = "&status=eq.delivered";
        } else if (filter == OrderFilter.CANCELLED) {
            filterQuery = "&status=eq.cancelled";
        }

        String url = SupabaseConfig.REST_URL + "/orders?user_id=eq." + userId 
                + filterQuery 
                + "&select=*,restaurants(name,address)&order=created_at.desc";

        Request request = authorizedBuilder(sessionManager, url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, "Failed to load orders: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = readBody(response);
                if (!response.isSuccessful()) {
                    postError(callback, "Failed to load orders (" + response.code() + ")");
                    return;
                }

                JsonArray array = JsonParser.parseString(body).getAsJsonArray();
                List<OrderSummary> orders = new ArrayList<>();
                for (JsonElement element : array) {
                    orders.add(parseOrderSummary(element.getAsJsonObject()));
                }
                postSuccess(callback, orders);
            }
        });
    }

    public void getOrderById(Context context, String orderId, RepositoryCallback<OrderSummary> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }

        String url = SupabaseConfig.REST_URL + "/orders?id=eq." + orderId 
                + "&select=*,restaurants(name,address),order_items(*)";

        Request request = authorizedBuilder(sessionManager, url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, "Failed to load order: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = readBody(response);
                if (!response.isSuccessful()) {
                    postError(callback, "Failed to load order (" + response.code() + ")");
                    return;
                }

                JsonArray array = JsonParser.parseString(body).getAsJsonArray();
                if (array.size() == 0) {
                    postError(callback, "Order not found");
                    return;
                }

                postSuccess(callback, parseOrderSummary(array.get(0).getAsJsonObject()));
            }
        });
    }

    public void validatePromotion(Context context, String code, int subtotal, RepositoryCallback<PromotionValidationResult> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        String url = SupabaseConfig.REST_URL + "/rpc/rpc_apply_promotion";
        
        JsonObject payload = new JsonObject();
        payload.addProperty("p_promotion_code", code);
        payload.addProperty("p_subtotal", subtotal);

        Request request = authorizedBuilder(sessionManager, url)
                .post(RequestBody.create(payload.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, "Lỗi kết nối: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = readBody(response);
                if (!response.isSuccessful()) {
                    postError(callback, "Không thể xác thực mã (" + response.code() + ")");
                    return;
                }

                try {
                    JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                    PromotionValidationResult result = new PromotionValidationResult(
                            obj.get("valid").getAsBoolean(),
                            obj.get("message").getAsString(),
                            obj.get("discount").getAsInt(),
                            getAsString(obj, "promotion_id")
                    );
                    postSuccess(callback, result);
                } catch (Exception e) {
                    postError(callback, "Lỗi phân tích dữ liệu: " + e.getMessage());
                }
            }
        });
    }

    public void createOrder(
            Context context,
            String restaurantId,
            String deliveryAddress,
            double lat,
            double lon,
            String note,
            String paymentMethod,
            String promotionCode,
            JsonArray items,
            RepositoryCallback<OrderSummary> callback
    ) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }

        String url = SupabaseConfig.REST_URL + "/rpc/rpc_create_order";
        JsonObject payload = new JsonObject();
        payload.addProperty("p_user_id", sessionManager.getUserId());
        payload.addProperty("p_restaurant_id", restaurantId);
        payload.addProperty("p_delivery_address", deliveryAddress);
        payload.addProperty("p_delivery_latitude", lat);
        payload.addProperty("p_delivery_longitude", lon);
        payload.addProperty("p_note", note);
        payload.addProperty("p_payment_method", paymentMethod != null ? paymentMethod : "cod");
        payload.addProperty("p_promotion_code", promotionCode);
        payload.add("p_items", items);

        Request request = authorizedBuilder(sessionManager, url)
                .post(RequestBody.create(payload.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, "Failed to create order: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = readBody(response);
                if (!response.isSuccessful()) {
                    postError(callback, "Failed to create order (" + response.code() + "): " + body);
                    return;
                }

                // RPC returns the created order object or ID. Let's assume it returns the order object or at least ID.
                // Based on migration, it returns JSONB (the order object after insertion)
                try {
                    JsonObject result = JsonParser.parseString(body).getAsJsonObject();
                    // If result only contains ID, we might need to fetch it again, 
                    // but usually RPC returns what we need.
                    // Let's reload to be sure we have all joins.
                    String orderId = getAsString(result, "id");
                    if (orderId.isEmpty()) {
                        postError(callback, "Created order has no ID.");
                    } else {
                        getOrderById(context, orderId, callback);
                    }
                } catch (Exception e) {
                    postError(callback, "Failed to parse created order: " + e.getMessage());
                }
            }
        });
    }

    private OrderSummary parseOrderSummary(JsonObject obj) {
        String id = getAsString(obj, "id");
        String code = getAsString(obj, "order_code");
        String status = getAsString(obj, "status");
        String deliveryStatus = getAsString(obj, "delivery_status");
        String createdAt = getAsString(obj, "created_at");
        int subtotal = getAsInt(obj, "subtotal");
        int deliveryFee = getAsInt(obj, "delivery_fee");
        int discount = getAsInt(obj, "discount");
        int total = getAsInt(obj, "total");
        String address = getAsString(obj, "delivery_address");
        String note = getAsString(obj, "note");
        String paymentMethod = getAsString(obj, "payment_method");
        String paymentStatus = getAsString(obj, "payment_status");

        JsonObject restaurant = obj.getAsJsonObject("restaurants");
        String restName = restaurant != null ? getAsString(restaurant, "name") : "Unknown";
        String restAddress = restaurant != null ? getAsString(restaurant, "address") : "";

        List<OrderLineItem> items = new ArrayList<>();
        if (obj.has("order_items") && obj.get("order_items").isJsonArray()) {
            JsonArray itemsArray = obj.getAsJsonArray("order_items");
            for (JsonElement itemEl : itemsArray) {
                JsonObject itemObj = itemEl.getAsJsonObject();
                items.add(new OrderLineItem(
                        getAsString(itemObj, "menu_item_name"),
                        getAsInt(itemObj, "quantity"),
                        getAsInt(itemObj, "menu_item_price")
                ));
            }
        }

        return new OrderSummary(id, code, restName, restAddress, status, deliveryStatus, createdAt,
                subtotal, deliveryFee, discount, total, address, note, items, paymentMethod, paymentStatus);
    }

    private Request.Builder authorizedBuilder(SessionManager sessionManager, String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON);
    }

    private void postSuccess(RepositoryCallback<?> callback, Object result) {
        mainHandler.post(() -> {
            @SuppressWarnings("unchecked")
            RepositoryCallback<Object> casted = (RepositoryCallback<Object>) callback;
            casted.onSuccess(result);
        });
    }

    private void postError(RepositoryCallback<?> callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }

    private String readBody(Response response) throws IOException {
        return response.body() == null ? "" : response.body().string();
    }

    private String getAsString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private int getAsInt(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return 0;
        }
        return object.get(key).getAsInt();
    }
}
