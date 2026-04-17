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
import com.utt.foodcouriers_client.data.model.CartItem;
import com.utt.foodcouriers_client.data.model.CartRestaurantGroup;
import com.utt.foodcouriers_client.data.model.MenuItem;
import com.utt.foodcouriers_client.data.model.Restaurant;
import com.utt.foodcouriers_client.data.remote.SupabaseConfig;
import com.utt.foodcouriers_client.utils.CartDTO.CartMeta;
import com.utt.foodcouriers_client.utils.CartDTO.CartState;
import com.utt.foodcouriers_client.utils.CartDTO.CartSummary;
import com.utt.foodcouriers_client.utils.CartDTO.MutableRestaurantGroup;
import com.utt.foodcouriers_client.utils.SessionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Repository xử lý toàn bộ nghiệp vụ dữ liệu của giỏ hàng.
 *
 * <p>Trách nhiệm chính:
 * <ul>
 *     <li>Kiểm tra trạng thái đăng nhập.</li>
 *     <li>Tìm cart hiện tại hoặc tạo cart mới nếu cần.</li>
 *     <li>Thêm, cập nhật, xóa cart item qua Supabase REST API.</li>
 *     <li>Lấy danh sách cart item và chuyển đổi JSON thành model Java.</li>
 *     <li>Tính toán summary cart và gom item theo nhà hàng.</li>
 *     <li>Trả callback về main thread để tầng UI có thể cập nhật an toàn.</li>
 * </ul>
 */
public class CartRepository {

    private static final MediaType JSON = MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON);
    private static CartRepository instance;

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Trả về singleton instance của {@code CartRepository}.
     *
     * @return instance dùng chung của repository
     */
    public static synchronized CartRepository getInstance() {
        if (instance == null) {
            instance = new CartRepository();
        }
        return instance;
    }

    /**
     * Kiểm tra người dùng hiện tại đã đăng nhập hay chưa.
     *
     * @param context context dùng để truy cập SessionManager
     * @return true nếu đã đăng nhập, ngược lại là false
     */
    public boolean isLoggedIn(Context context) {
        return SessionManager.getInstance(context).isLoggedIn();
    }

    /**
     * Lấy trạng thái cart hiện tại của người dùng.
     *
     * <p>Nếu chưa đăng nhập thì callback lỗi {@code AUTH_REQUIRED}.
     * Nếu đã đăng nhập thì repository sẽ tìm cart hiện tại và tải toàn bộ dữ liệu cart.
     *
     * @param context context hiện tại
     * @param callback callback nhận kết quả {@code CartState}
     */
    public void getCart(Context context, RepositoryCallback<CartState> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }

        fetchCartState(context, callback);
    }

    /**
     * Thêm một menu item vào cart theo cơ chế cộng dồn số lượng.
     *
     * <p>Luồng xử lý:
     * <ol>
     *     <li>Kiểm tra đăng nhập và validate dữ liệu đầu vào.</li>
     *     <li>Tìm cart hiện tại hoặc tạo cart mới.</li>
     *     <li>Load state hiện tại để kiểm tra món này đã tồn tại trong cart chưa.</li>
     *     <li>Nếu đã tồn tại thì tăng quantity của cart item hiện có.</li>
     *     <li>Nếu chưa tồn tại thì upsert item mới vào cart.</li>
     *     <li>Load lại toàn bộ cart state sau khi xử lý xong.</li>
     * </ol>
     *
     * @param context context hiện tại
     * @param menuItem món cần thêm
     * @param restaurant nhà hàng của món; hiện chưa được dùng trong logic
     * @param quantity số lượng muốn cộng thêm
     * @param note ghi chú cho item
     * @param callback callback nhận trạng thái cart mới nhất
     */
    public void addToCart(
            Context context,
            MenuItem menuItem,
            Restaurant restaurant,
            int quantity,
            String note,
            RepositoryCallback<CartState> callback
    ) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }
        if (menuItem == null || quantity <= 0) {
            postError(callback, "Invalid cart payload.");
            return;
        }

        getOrCreateCart(context, new RepositoryCallback<CartMeta>() {
            @Override
            public void onSuccess(CartMeta cartMeta) {
                fetchCartState(context, new RepositoryCallback<CartState>() {
                    @Override
                    public void onSuccess(CartState state) {
                        CartItem existingItem = findCartItemByMenuItemId(state.getItems(), menuItem.getId());
                        if (existingItem != null) {
                            updateCartItemQuantity(context, existingItem.getId(), existingItem.getQuantity() + quantity, callback);
                            return;
                        }
                        upsertCartItem(context, cartMeta.getCartId(), menuItem.getId(), quantity, note, new RepositoryCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                fetchCartState(context, callback);
                            }

                            @Override
                            public void onError(String error) {
                                postError(callback, error);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        postError(callback, error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                postError(callback, error);
            }
        });
    }

    /**
     * Đặt số lượng cuối cùng cho một menu item trong cart.
     *
     * <p>Khác với {@link #addToCart}, hàm này mang ý nghĩa "set exact quantity".
     * Nếu quantity <= 0 thì item sẽ bị xóa khỏi cart.
     *
     * @param context context hiện tại
     * @param menuItem món cần cập nhật
     * @param restaurant nhà hàng của món; hiện chưa được dùng trong logic
     * @param quantity số lượng cuối cùng cần đặt
     * @param note ghi chú của item
     * @param callback callback nhận trạng thái cart mới nhất
     */
    public void setMenuItemQuantity(
            Context context,
            MenuItem menuItem,
            Restaurant restaurant,
            int quantity,
            String note,
            RepositoryCallback<CartState> callback
    ) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }
        if (menuItem == null) {
            postError(callback, "Invalid cart payload.");
            return;
        }

        String cachedCartId = sessionManager.getCartId();

        if (cachedCartId != null && !cachedCartId.isEmpty()) {
            performUpsertAndFetchState(context, cachedCartId, menuItem.getId(), quantity, note, callback);
        } else {
            getOrCreateCart(context, new RepositoryCallback<CartMeta>() {
                @Override
                public void onSuccess(CartMeta cartMeta) {
                    if (cartMeta != null && cartMeta.getCartId() != null) {
                        sessionManager.setCartId(cartMeta.getCartId());
                    }
                    performUpsertAndFetchState(context, cartMeta.getCartId(), menuItem.getId(), quantity, note, callback);
                }

                @Override
                public void onError(String error) {
                    postError(callback, error);
                }
            });
        }
    }

    /**
     * Thực hiện upsert item rồi load lại cart state.
     *
     * <p>Nếu quantity <= 0 thì chuyển sang nhánh xóa item khỏi cart.
     *
     * @param context context hiện tại
     * @param cartId id cart đang thao tác
     * @param menuItemId id món ăn
     * @param quantity số lượng cần set
     * @param note ghi chú item
     * @param callback callback nhận cart state mới nhất
     */
    private void performUpsertAndFetchState(
            Context context,
            String cartId,
            String menuItemId,
            int quantity,
            String note,
            RepositoryCallback<CartState> callback
    ) {
        if (quantity <= 0) {
            fetchCartStateForDelete(context, menuItemId, callback);
            return;
        }

        upsertCartItemOptimized(context, cartId, menuItemId, quantity, note, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                fetchCartState(context, callback);
            }

            @Override
            public void onError(String error) {
                postError(callback, error);
            }
        });
    }

    /**
     * Load cart state hiện tại rồi tìm item theo menuItemId để xóa.
     *
     * <p>Nhánh này được dùng khi muốn set quantity về 0 hoặc nhỏ hơn.
     *
     * @param context context hiện tại
     * @param menuItemId id món ăn cần xóa khỏi cart
     * @param callback callback nhận trạng thái cart sau khi xóa
     */
    private void fetchCartStateForDelete(Context context, String menuItemId, RepositoryCallback<CartState> callback) {
        fetchCartState(context, new RepositoryCallback<CartState>() {
            @Override
            public void onSuccess(CartState state) {
                CartItem existingItem = findCartItemByMenuItemId(state.getItems(), menuItemId);
                if (existingItem == null) {
                    postSuccess(callback, state);
                    return;
                }
                removeItem(context, existingItem.getId(), callback);
            }

            @Override
            public void onError(String error) {
                postError(callback, error);
            }
        });
    }

    /**
     * Upsert một cart item theo cặp khóa {@code cart_id + menu_item_id}.
     *
     * <p>Supabase sẽ merge nếu item đã tồn tại thay vì tạo bản ghi trùng.
     *
     * @param context context hiện tại
     * @param cartId id cart
     * @param menuItemId id món ăn
     * @param quantity số lượng cần lưu
     * @param note ghi chú item
     * @param callback callback báo thành công/thất bại
     */
    private void upsertCartItemOptimized(
            Context context,
            String cartId,
            String menuItemId,
            int quantity,
            String note,
            RepositoryCallback<Boolean> callback
    ) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        String url = SupabaseConfig.REST_URL + "/cart_items?on_conflict=cart_id,menu_item_id";

        JsonObject body = new JsonObject();
        body.addProperty("cart_id", cartId);
        body.addProperty("menu_item_id", menuItemId);
        body.addProperty("quantity", quantity);
        if (note != null && !note.trim().isEmpty()) {
            body.addProperty("note", note.trim());
        }

        Request request = authorizedBuilder(sessionManager, url)
                .addHeader(SupabaseConfig.HEADER_PREFER, "resolution=merge-duplicates," + SupabaseConfig.PREF_RETURN_REPRESENTATION)
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, "Failed to add item to cart: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = readBody(response);
                if (!response.isSuccessful()) {
                    postError(callback, "Failed to add item to cart (" + response.code() + ")");
                    return;
                }
                postSuccess(callback, true);
            }
        });
    }

    /**
     * Cập nhật quantity của một cart item đã tồn tại theo {@code cartItemId}.
     *
     * <p>Nếu quantity <= 0 thì item sẽ bị xóa khỏi cart.
     *
     * @param context context hiện tại
     * @param cartItemId id của bản ghi cart item
     * @param quantity số lượng mới
     * @param callback callback nhận trạng thái cart mới nhất
     */
    public void updateCartItemQuantity(
            Context context,
            String cartItemId,
            int quantity,
            RepositoryCallback<CartState> callback
    ) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }
        if (cartItemId == null || cartItemId.trim().isEmpty()) {
            postError(callback, "Invalid cart item.");
            return;
        }

        if (quantity <= 0) {
            removeItem(context, cartItemId, callback);
            return;
        }

        String url = SupabaseConfig.REST_URL + "/cart_items?id=eq." + cartItemId;
        JsonObject body = new JsonObject();
        body.addProperty("quantity", quantity);

        Request request = authorizedBuilder(sessionManager, url)
                .patch(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, "Failed to update cart item: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = readBody(response);
                if (!response.isSuccessful()) {
                    postError(callback, "Failed to update cart item (" + response.code() + ")");
                    return;
                }
                fetchCartState(context, callback);
            }
        });
    }

    public void removeItem(Context context, String cartItemId, RepositoryCallback<CartState> callback) {
        removeItems(context, java.util.Collections.singletonList(cartItemId), callback);
    }

    /**
     * Xóa nhiều cart item khỏi cart trong một request.
     *
     * @param context context hiện tại
     * @param cartItemIds danh sách id cart item cần xóa
     * @param callback callback nhận trạng thái cart sau khi xóa
     */
    public void removeItems(Context context, List<String> cartItemIds, RepositoryCallback<CartState> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            postError(callback, "Invalid cart item list.");
            return;
        }

        String idsParam = String.join(",", cartItemIds);
        String url = SupabaseConfig.REST_URL + "/cart_items?id=in.(" + idsParam + ")";
        Request request = authorizedBuilder(sessionManager, url)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, "Failed to remove cart items: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = readBody(response);
                if (!response.isSuccessful()) {
                    postError(callback, "Failed to remove cart items (" + response.code() + ")");
                    return;
                }
                fetchCartState(context, callback);
            }
        });
    }

    /**
     * Xóa toàn bộ item thuộc cart hiện tại của người dùng.
     *
     * <p>Nếu người dùng chưa có cart thì coi như clear thành công.
     *
     * @param context context hiện tại
     * @param callback callback nhận true nếu xử lý xong
     */
    public void clearCart(Context context, RepositoryCallback<Boolean> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }

        getCurrentCart(context, new RepositoryCallback<CartMeta>() {
            @Override
            public void onSuccess(CartMeta cartMeta) {
                if (cartMeta == null) {
                    postSuccess(callback, true);
                    return;
                }

                String url = SupabaseConfig.REST_URL + "/cart_items?cart_id=eq." + cartMeta.getCartId();
                Request request = authorizedBuilder(sessionManager, url)
                        .delete()
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        postError(callback, "Failed to clear cart: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String responseBody = readBody(response);
                        if (!response.isSuccessful()) {
                            postError(callback, "Failed to clear cart (" + response.code() + ")");
                            return;
                        }
                        postSuccess(callback, true);
                    }
                });
            }

            @Override
            public void onError(String error) {
                postError(callback, error);
            }
        });
    }

    /**
     * Tải trạng thái cart tổng quát của người dùng.
     *
     * <p>Nếu không tìm thấy cart thì trả về {@code CartState.empty()}.
     * Nếu có cart thì tiếp tục lấy toàn bộ cart item và summary.
     *
     * @param context context hiện tại
     * @param callback callback nhận cart state
     */
    private void fetchCartState(Context context, RepositoryCallback<CartState> callback) {
        getCurrentCart(context, new RepositoryCallback<CartMeta>() {
            @Override
            public void onSuccess(CartMeta cartMeta) {
                if (cartMeta == null) {
                    postSuccess(callback, CartState.empty());
                    return;
                }

                fetchCartItems(context, cartMeta, callback);
            }

            @Override
            public void onError(String error) {
                postError(callback, error);
            }
        });
    }

    /**
     * Tải toàn bộ item của cart hiện tại và chuyển đổi dữ liệu JSON thành model Java.
     *
     * <p>Hàm này đồng thời:
     * <ul>
     *     <li>Gom item theo restaurant.</li>
     *     <li>Tính itemCount, subtotal, deliveryFee và total.</li>
     *     <li>Tạo {@code CartSummary} và {@code CartState} hoàn chỉnh cho tầng UI.</li>
     * </ul>
     *
     * @param context context hiện tại
     * @param cartMeta thông tin cart đang cần tải dữ liệu
     * @param callback callback nhận cart state hoàn chỉnh
     */
    private void fetchCartItems(Context context, CartMeta cartMeta, RepositoryCallback<CartState> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        String url = SupabaseConfig.REST_URL
                + "/cart_items?cart_id=eq."
                + cartMeta.getCartId()
                + "&select=id,quantity,note,menu_items(id,restaurant_id,name,price,image_url,restaurants(id,name,delivery_fee,latitude,longitude))"
                + "&order=id";

        Request request = authorizedBuilder(sessionManager, url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, "Failed to load cart items: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = readBody(response);
                if (!response.isSuccessful()) {
                    postError(callback, "Failed to load cart items (" + response.code() + ")");
                    return;
                }

                JsonArray itemsArray = JsonParser.parseString(body).getAsJsonArray();
                List<CartItem> items = new ArrayList<>();
                LinkedHashMap<String, MutableRestaurantGroup> groups = new LinkedHashMap<>();
                int itemCount = 0;
                int subtotal = 0;
                Map<String, Integer> menuQuantities = new LinkedHashMap<>();

                for (JsonElement element : itemsArray) {
                    JsonObject obj = element.getAsJsonObject();
                    JsonObject menu = obj.getAsJsonObject("menu_items");
                    if (menu == null) {
                        continue;
                    }

                    CartItem item = new CartItem();
                    item.setId(getAsString(obj, "id"));
                    item.setQuantity(getAsInt(obj, "quantity"));
                    item.setNote(getAsString(obj, "note"));

                    MenuItem menuItem = new MenuItem();
                    menuItem.setId(getAsString(menu, "id"));
                    menuItem.setRestaurantId(getAsString(menu, "restaurant_id"));
                    menuItem.setName(getAsString(menu, "name"));
                    menuItem.setPrice(getAsInt(menu, "price"));
                    menuItem.setImageUrl(getAsString(menu, "image_url"));
                    item.setMenuItem(menuItem);
                    JsonObject restaurant = menu.has("restaurants") && menu.get("restaurants").isJsonObject()
                            ? menu.getAsJsonObject("restaurants")
                            : null;
                    String restaurantId = menuItem.getRestaurantId();
                    String restaurantName = restaurant != null ? getAsString(restaurant, "name") : "";
                    int deliveryFee = restaurant != null ? getAsInt(restaurant, "delivery_fee") : 0;
                    item.setRestaurantName(restaurantName);
                    
                    Double restaurantLat = restaurant != null ? getAsDouble(restaurant, "latitude") : null;
                    Double restaurantLon = restaurant != null ? getAsDouble(restaurant, "longitude") : null;
                    item.setRestaurantLatitude(restaurantLat);
                    item.setRestaurantLongitude(restaurantLon);

                    items.add(item);
                    MutableRestaurantGroup group = groups.get(restaurantId);
                    if (group == null) {
                        group = new MutableRestaurantGroup(restaurantId, restaurantName, deliveryFee);
                        group.setRestaurantLatitude(restaurantLat);
                        group.setRestaurantLongitude(restaurantLon);
                        groups.put(restaurantId, group);
                    }
                    group.getItems().add(item);
                    itemCount += item.getQuantity();
                    subtotal += item.getQuantity() * item.getPrice();
                    menuQuantities.put(item.getMenuItemId(), item.getQuantity());
                }

                int deliveryFee = 0;
                List<CartRestaurantGroup> restaurantGroups = new ArrayList<>();
                for (MutableRestaurantGroup group : groups.values()) {
                    restaurantGroups.add(new CartRestaurantGroup(
                            group.getRestaurantId(),
                            group.getRestaurantName(),
                            group.getDeliveryFee(),
                            0,
                            group.getItems(),
                            group.getRestaurantLatitude(),
                            group.getRestaurantLongitude()
                    ));
                    deliveryFee += group.getItems().isEmpty() ? 0 : group.getDeliveryFee();
                }
                int total = subtotal + deliveryFee;
                CartSummary summary = new CartSummary(
                        itemCount,
                        subtotal,
                        deliveryFee,
                        0,
                        0,
                        total,
                        ""
                );
                postSuccess(callback, new CartState(cartMeta.getCartId(), items, restaurantGroups, summary, menuQuantities));
            }
        });
    }

    /**
     * Tìm cart hiện tại; nếu chưa có thì tạo cart mới.
     *
     * @param context context hiện tại
     * @param callback callback nhận metadata của cart
     */
    private void getOrCreateCart(Context context, RepositoryCallback<CartMeta> callback) {
        getCurrentCart(context, new RepositoryCallback<CartMeta>() {
            @Override
            public void onSuccess(CartMeta cartMeta) {
                if (cartMeta == null) {
                    createCart(context, callback);
                    return;
                }
                postSuccess(callback, cartMeta);
            }

            @Override
            public void onError(String error) {
                postError(callback, error);
            }
        });
    }

    /**
     * Lấy cart hiện tại của người dùng.
     *
     * <p>Ưu tiên dùng {@code cartId} đã cache trong session.
     * Nếu cache không hợp lệ thì fallback sang tìm cart theo user hiện tại.
     *
     * @param context context hiện tại
     * @param callback callback nhận metadata của cart, hoặc null nếu chưa có cart
     */
    private void getCurrentCart(Context context, RepositoryCallback<CartMeta> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        String userId = sessionManager.getUserId();
        String authUserId = sessionManager.getAuthUserId();
        if (userId == null || userId.trim().isEmpty()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }

        String cachedCartId = sessionManager.getCartId();
        if (cachedCartId != null && !cachedCartId.isEmpty()) {
            String cachedCartUrl = SupabaseConfig.REST_URL
                    + "/carts?id=eq."
                    + cachedCartId
                    + "&select=id&limit=1";
            fetchCartMeta(context, sessionManager, cachedCartUrl, new RepositoryCallback<CartMeta>() {
                @Override
                public void onSuccess(CartMeta result) {
                    if (result != null) {
                        postSuccess(callback, result);
                        return;
                    }
                    sessionManager.setCartId(null);
                    fetchOwnedCart(context, sessionManager, userId, authUserId, callback);
                }

                @Override
                public void onError(String error) {
                    postError(callback, error);
                }
            });
            return;
        }

        fetchOwnedCart(context, sessionManager, userId, authUserId, callback);
    }

    /**
     * Tạo cart mới cho người dùng hiện tại trên Supabase.
     *
     * <p>Sau khi tạo thành công, {@code cartId} sẽ được cache vào {@code SessionManager}.
     *
     * @param context context hiện tại
     * @param callback callback nhận metadata của cart mới tạo
     */
    private void createCart(Context context, RepositoryCallback<CartMeta> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        String userId = sessionManager.getUserId();
        String authUserId = sessionManager.getAuthUserId();
        if (userId == null || userId.trim().isEmpty()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }
        if (authUserId == null || authUserId.trim().isEmpty()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }

        String url = SupabaseConfig.REST_URL + "/carts";
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("user_auth_id", authUserId);

        Request request = authorizedBuilder(sessionManager, url)
                .addHeader(SupabaseConfig.HEADER_PREFER, SupabaseConfig.PREF_RETURN_REPRESENTATION)
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, "Failed to create cart: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = readBody(response);
                if (!response.isSuccessful()) {
                    postError(callback, "Failed to create cart (" + response.code() + ")");
                    return;
                }

                JsonArray array = JsonParser.parseString(responseBody).getAsJsonArray();
                if (array.size() == 0) {
                    postError(callback, "Cart was created without a payload.");
                    return;
                }

                JsonObject cart = array.get(0).getAsJsonObject();
                String cartId = getAsString(cart, "id");
                if (cartId != null) {
                    SessionManager.getInstance(context).setCartId(cartId);
                }
                postSuccess(callback, new CartMeta(cartId));
            }
        });
    }

    /**
     * Upsert cart item theo cặp khóa {@code cart_id + menu_item_id}.
     *
     * <p>Hàm này được dùng trong flow addToCart truyền thống.
     *
     * @param context context hiện tại
     * @param cartId id cart
     * @param menuItemId id món ăn
     * @param quantity số lượng cần thêm/cập nhật
     * @param note ghi chú item
     * @param callback callback báo kết quả thao tác
     */
    private void upsertCartItem(
            Context context,
            String cartId,
            String menuItemId,
            int quantity,
            String note,
            RepositoryCallback<Boolean> callback
    ) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        String url = SupabaseConfig.REST_URL + "/cart_items?on_conflict=cart_id,menu_item_id";

        JsonObject body = new JsonObject();
        body.addProperty("cart_id", cartId);
        body.addProperty("menu_item_id", menuItemId);
        body.addProperty("quantity", quantity);
        if (note != null && !note.trim().isEmpty()) {
            body.addProperty("note", note.trim());
        }

        Request request = authorizedBuilder(sessionManager, url)
                .addHeader(SupabaseConfig.HEADER_PREFER, "resolution=merge-duplicates," + SupabaseConfig.PREF_RETURN_REPRESENTATION)
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, "Failed to add item to cart: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = readBody(response);
                if (!response.isSuccessful()) {
                    postError(callback, "Failed to add item to cart (" + response.code() + ")");
                    return;
                }
                postSuccess(callback, true);
            }
        });
    }

    /**
     * Tìm cart item trong danh sách item theo {@code menuItemId}.
     *
     * @param items danh sách cart item hiện tại
     * @param menuItemId id món ăn cần tìm
     * @return cart item tương ứng; trả về null nếu không tìm thấy
     */
    private CartItem findCartItemByMenuItemId(List<CartItem> items, String menuItemId) {
        if (items == null || menuItemId == null) {
            return null;
        }
        for (CartItem item : items) {
            if (menuItemId.equals(item.getMenuItemId())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Tạo request builder đã gắn sẵn các header xác thực cho Supabase.
     *
     * @param sessionManager session hiện tại chứa access token
     * @param url endpoint cần gọi
     * @return builder đã có apikey, authorization và content-type
     */
    private Request.Builder authorizedBuilder(SessionManager sessionManager, String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON);
    }

    /**
     * Tạo query string dùng để tra cứu cart của người dùng hiện tại.
     *
     * <p>Ưu tiên tra theo {@code user_auth_id}. Nếu không có thì fallback sang {@code user_id}.
     *
     * @param userId id user nội bộ
     * @param authUserId id user xác thực
     * @return query string dùng cho endpoint carts
     */
    private String buildCartLookupQuery(String userId, String authUserId) {
        if (authUserId != null && !authUserId.trim().isEmpty()) {
            return "user_auth_id=eq." + authUserId;
        }
        return "user_id=eq." + userId;
    }

    /**
     * Tải cart thuộc sở hữu của user hiện tại từ Supabase.
     *
     * @param context context hiện tại
     * @param sessionManager session hiện tại
     * @param userId id user nội bộ
     * @param authUserId id user xác thực
     * @param callback callback nhận metadata của cart
     */
    private void fetchOwnedCart(
            Context context,
            SessionManager sessionManager,
            String userId,
            String authUserId,
            RepositoryCallback<CartMeta> callback
    ) {
        String url = SupabaseConfig.REST_URL
                + "/carts?"
                + buildCartLookupQuery(userId, authUserId)
                + "&select=id&limit=1";
        fetchCartMeta(context, sessionManager, url, callback);
    }

    /**
     * Gọi endpoint carts và parse metadata cart.
     *
     * @param context context hiện tại
     * @param sessionManager session hiện tại
     * @param url endpoint truy vấn cart
     * @param callback callback nhận metadata của cart hoặc null nếu không có cart
     */
    private void fetchCartMeta(
            Context context,
            SessionManager sessionManager,
            String url,
            RepositoryCallback<CartMeta> callback
    ) {
        Request request = authorizedBuilder(sessionManager, url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, "Failed to load cart: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = readBody(response);
                if (!response.isSuccessful()) {
                    postError(callback, "Failed to load cart (" + response.code() + ")");
                    return;
                }

                JsonArray array = JsonParser.parseString(body).getAsJsonArray();
                if (array.size() == 0) {
                    postSuccess(callback, null);
                    return;
                }

                JsonObject cart = array.get(0).getAsJsonObject();
                String cartId = getAsString(cart, "id");
                if (cartId != null && !cartId.isEmpty()) {
                    sessionManager.setCartId(cartId);
                }
                postSuccess(callback, new CartMeta(cartId));
            }
        });
    }

    /**
     * Đảm bảo callback success luôn được trả về main thread.
     *
     * @param callback callback đích
     * @param result dữ liệu kết quả
     */
    private void postSuccess(RepositoryCallback<?> callback, Object result) {
        mainHandler.post(() -> {
            @SuppressWarnings("unchecked")
            RepositoryCallback<Object> casted = (RepositoryCallback<Object>) callback;
            casted.onSuccess(result);
        });
    }

    /**
     * Đảm bảo callback error luôn được trả về main thread.
     *
     * @param callback callback đích
     * @param error thông điệp lỗi
     */
    private void postError(RepositoryCallback<?> callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }

    /**
     * Đọc toàn bộ response body thành chuỗi.
     *
     * @param response response từ network
     * @return nội dung body; trả về chuỗi rỗng nếu body null
     * @throws IOException khi đọc body thất bại
     */
    private String readBody(Response response) throws IOException {
        return response.body() == null ? "" : response.body().string();
    }

    /**
     * Đọc một giá trị String từ JsonObject một cách an toàn.
     *
     * @param object object nguồn
     * @param key key cần đọc
     * @return giá trị String; trả về chuỗi rỗng nếu key không tồn tại hoặc null
     */
    private String getAsString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    /**
     * Đọc một giá trị int từ JsonObject một cách an toàn.
     *
     * @param object object nguồn
     * @param key key cần đọc
     * @return giá trị int; trả về 0 nếu key không tồn tại hoặc null
     */
    private int getAsInt(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return 0;
        }
        return object.get(key).getAsInt();
    }

    /**
     * Đọc một giá trị Double từ JsonObject một cách an toàn.
     *
     * @param object object nguồn
     * @param key key cần đọc
     * @return giá trị Double; trả về null nếu key không tồn tại hoặc null
     */
    private Double getAsDouble(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.get(key).getAsDouble();
    }
}
