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

public class CartRepository {

    private static final MediaType JSON = MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON);
    private static CartRepository instance;

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static synchronized CartRepository getInstance() {
        if (instance == null) {
            instance = new CartRepository();
        }
        return instance;
    }

    public boolean isLoggedIn(Context context) {
        return SessionManager.getInstance(context).isLoggedIn();
    }

    public void getCart(Context context, RepositoryCallback<CartState> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }

        fetchCartState(context, callback);
    }

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
        SessionManager sessionManager = SessionManager.getInstance(context);
        if (!sessionManager.isLoggedIn()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }
        if (cartItemId == null || cartItemId.trim().isEmpty()) {
            postError(callback, "Invalid cart item.");
            return;
        }

        String url = SupabaseConfig.REST_URL + "/cart_items?id=eq." + cartItemId;
        Request request = authorizedBuilder(sessionManager, url)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, "Failed to remove cart item: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = readBody(response);
                if (!response.isSuccessful()) {
                    postError(callback, "Failed to remove cart item (" + response.code() + ")");
                    return;
                }
                fetchCartState(context, callback);
            }
        });
    }

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

    private void fetchCartItems(Context context, CartMeta cartMeta, RepositoryCallback<CartState> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        String url = SupabaseConfig.REST_URL
                + "/cart_items?cart_id=eq."
                + cartMeta.getCartId()
                + "&select=id,quantity,note,menu_items(id,restaurant_id,name,price,image_url,restaurants(id,name,delivery_fee))"
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

                    items.add(item);
                    MutableRestaurantGroup group = groups.get(restaurantId);
                    if (group == null) {
                        group = new MutableRestaurantGroup(restaurantId, restaurantName, deliveryFee);
                        groups.put(restaurantId, group);
                    }
                    group.items.add(item);
                    itemCount += item.getQuantity();
                    subtotal += item.getQuantity() * item.getPrice();
                    menuQuantities.put(item.getMenuItemId(), item.getQuantity());
                }

                int deliveryFee = 0;
                List<CartRestaurantGroup> restaurantGroups = new ArrayList<>();
                for (MutableRestaurantGroup group : groups.values()) {
                    restaurantGroups.add(new CartRestaurantGroup(group.restaurantId, group.restaurantName, group.deliveryFee, group.items));
                    deliveryFee += group.items.isEmpty() ? 0 : group.deliveryFee;
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

    private void getCurrentCart(Context context, RepositoryCallback<CartMeta> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        String userId = sessionManager.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }

        String cachedCartId = sessionManager.getCartId();
        if (cachedCartId != null && !cachedCartId.isEmpty()) {
            postSuccess(callback, new CartMeta(cachedCartId));
            return;
        }

        String url = SupabaseConfig.REST_URL
                + "/carts?user_id=eq."
                + userId
                + "&select=id&limit=1";

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
                if (cartId != null) {
                    SessionManager.getInstance(context).setCartId(cartId);
                }
                postSuccess(callback, new CartMeta(cartId));
            }
        });
    }

    private void createCart(Context context, RepositoryCallback<CartMeta> callback) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        String userId = sessionManager.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            postError(callback, "AUTH_REQUIRED");
            return;
        }

        String url = SupabaseConfig.REST_URL + "/carts";
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);

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

    public static class CartSummary {
        private final int itemCount;
        private final int subtotal;
        private final int deliveryFee;
        private final int serviceFee;
        private final int savings;
        private final int total;
        private final String restaurantName;

        public CartSummary(int itemCount, int subtotal, int deliveryFee, int serviceFee, int savings, int total, String restaurantName) {
            this.itemCount = itemCount;
            this.subtotal = subtotal;
            this.deliveryFee = deliveryFee;
            this.serviceFee = serviceFee;
            this.savings = savings;
            this.total = total;
            this.restaurantName = restaurantName;
        }

        public int getItemCount() {
            return itemCount;
        }

        public int getSubtotal() {
            return subtotal;
        }

        public int getDeliveryFee() {
            return deliveryFee;
        }

        public int getServiceFee() {
            return serviceFee;
        }

        public int getSavings() {
            return savings;
        }

        public int getTotal() {
            return total;
        }

        public String getRestaurantName() {
            return restaurantName;
        }
    }

    public static class CartState {
        private final String cartId;
        private final List<CartItem> items;
        private final List<CartRestaurantGroup> restaurantGroups;
        private final CartSummary summary;
        private final Map<String, Integer> menuItemQuantities;

        public CartState(String cartId, List<CartItem> items, List<CartRestaurantGroup> restaurantGroups, CartSummary summary, Map<String, Integer> menuItemQuantities) {
            this.cartId = cartId;
            this.items = items;
            this.restaurantGroups = restaurantGroups;
            this.summary = summary;
            this.menuItemQuantities = menuItemQuantities;
        }

        public static CartState empty() {
            return new CartState("", new ArrayList<>(), new ArrayList<>(), new CartSummary(0, 0, 0, 0, 0, 0, ""), new LinkedHashMap<>());
        }

        public String getCartId() {
            return cartId;
        }

        public List<CartItem> getItems() {
            return items;
        }

        public List<CartRestaurantGroup> getRestaurantGroups() {
            return restaurantGroups;
        }

        public CartSummary getSummary() {
            return summary;
        }

        public Map<String, Integer> getMenuItemQuantities() {
            return menuItemQuantities;
        }
    }

    public static class CartMeta {
        private final String cartId;

        public CartMeta(String cartId) {
            this.cartId = cartId;
        }

        public String getCartId() {
            return cartId;
        }
    }

    private static class MutableRestaurantGroup {
        private final String restaurantId;
        private final String restaurantName;
        private final int deliveryFee;
        private final List<CartItem> items = new ArrayList<>();

        private MutableRestaurantGroup(String restaurantId, String restaurantName, int deliveryFee) {
            this.restaurantId = restaurantId;
            this.restaurantName = restaurantName;
            this.deliveryFee = deliveryFee;
        }
    }
}
