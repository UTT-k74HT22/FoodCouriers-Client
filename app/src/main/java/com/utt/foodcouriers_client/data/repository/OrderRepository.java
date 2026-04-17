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

/**
 * Repository thao tác với đơn hàng qua Supabase REST/RPC.
 *
 * <p>Realtime trong UI chỉ đóng vai trò báo "dữ liệu đã thay đổi". Sau mỗi event realtime,
 * các màn order vẫn gọi repository này để kéo lại dữ liệu đầy đủ từ REST. Cách này giúp UI
 * không phụ thuộc vào payload realtime thiếu field join như restaurant hoặc order_items.</p>
 */
public class OrderRepository {

    private static final MediaType JSON = MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON);
    private static OrderRepository instance;

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Bộ lọc danh sách đơn hàng ở màn lịch sử đơn.
     */
    public enum OrderFilter {
        ALL,
        ACTIVE,
        COMPLETED,
        CANCELLED
    }

    /**
     * @return singleton repository dùng chung cho checkout, order history, detail và tracking
     */
    public static synchronized OrderRepository getInstance() {
        if (instance == null) {
            instance = new OrderRepository();
        }
        return instance;
    }
    /**
     * Lấy danh sách đơn hàng của user đang đăng nhập.
     *
     * @param context context dùng để lấy {@link SessionManager}
     * @param filter bộ lọc tab hiện tại
     * @param callback trả danh sách {@link OrderSummary} đã parse từ REST response
     */
    public void getOrders(Context context, OrderFilter filter, RepositoryCallback<List<OrderSummary>> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "Chức năng này cần đăng nhập");
            return;
        }

        String userId = sessionManager.getUserId();
        String filterQuery = "";
        if (filter == OrderFilter.ACTIVE) {
            filterQuery = "&status=in.(awaiting_payment,pending,confirmed,preparing,ready_for_pickup,delivering)";
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

    /**
     * Lấy chi tiết một đơn hàng, bao gồm thông tin nhà hàng và line items.
     *
     * @param context context dùng để lấy session/access token
     * @param orderId id đơn hàng cần load
     * @param callback trả {@link OrderSummary} đầy đủ cho màn detail/tracking
     */
    public void getOrderById(Context context, String orderId, RepositoryCallback<OrderSummary> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "Chức năng này cần đăng nhập");
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

    /**
     * Gọi RPC kiểm tra mã khuyến mãi trước khi tạo đơn.
     *
     * @param context context dùng để lấy session
     * @param code mã khuyến mãi user nhập
     * @param subtotal tổng tiền hàng trước phí/giảm giá
     * @param callback kết quả hợp lệ, message và discount
     */
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

    /**
     * Tạo đơn hàng bằng Supabase RPC {@code rpc_create_order}.
     *
     * <p>RPC chịu trách nhiệm insert orders/order_items, tính tổng tiền và có thể tạo notification.
     * Repository chỉ gửi payload từ checkout, sau đó load lại order bằng id RPC trả về để UI nhận
     * dữ liệu đầy đủ.</p>
     *
     * @param context context dùng để lấy user/session
     * @param restaurantId id nhà hàng
     * @param deliveryAddress địa chỉ giao hàng
     * @param lat vĩ độ giao hàng
     * @param lon kinh độ giao hàng
     * @param note ghi chú của user
     * @param paymentMethod phương thức thanh toán
     * @param promotionCode mã khuyến mãi nếu có
     * @param items danh sách món gửi vào RPC
     * @param deliveryFee phí giao hàng
     * @param callback trả order vừa tạo sau khi load lại từ REST
     */
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
            int deliveryFee,
            RepositoryCallback<OrderSummary> callback
    ) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "Chức năng này cần đăng nhập");
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
        payload.addProperty("p_delivery_fee", deliveryFee);

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
                try {
                    JsonObject result = JsonParser.parseString(body).getAsJsonObject();
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
    /**
     * Chuyển JSON REST response thành model dùng chung cho list/detail/tracking.
     *
     * @param obj object order từ Supabase REST
     * @return model {@link OrderSummary}
     */
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

    /**
     * Tạo request builder có đủ anon key, bearer token và content type.
     */
    private Request.Builder authorizedBuilder(SessionManager sessionManager, String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON);
    }

    /**
     * Đưa callback thành công về main thread để ViewModel cập nhật LiveData an toàn.
     */
    private void postSuccess(RepositoryCallback<?> callback, Object result) {
        mainHandler.post(() -> {
            @SuppressWarnings("unchecked")
            RepositoryCallback<Object> casted = (RepositoryCallback<Object>) callback;
            casted.onSuccess(result);
        });
    }

    /**
     * Đưa callback lỗi về main thread.
     */
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
