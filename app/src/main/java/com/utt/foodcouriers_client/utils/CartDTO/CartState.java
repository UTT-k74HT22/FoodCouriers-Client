package com.utt.foodcouriers_client.utils.CartDTO;

import com.utt.foodcouriers_client.data.model.CartItem;
import com.utt.foodcouriers_client.data.model.CartRestaurantGroup;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO mô tả trạng thái đầy đủ của cart sau khi repository xử lý xong.
 *
 * <p>Bao gồm:
 * cartId, danh sách item, danh sách nhóm nhà hàng, summary và map quantity theo menuItemId.
 */
public class CartState {
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
