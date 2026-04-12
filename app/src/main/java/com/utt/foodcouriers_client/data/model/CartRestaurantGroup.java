package com.utt.foodcouriers_client.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CartRestaurantGroup implements Serializable {

    private final String restaurantId;
    private final String restaurantName;
    private final int deliveryFee;
    private final List<CartItem> items;

    public CartRestaurantGroup(String restaurantId, String restaurantName, int deliveryFee, List<CartItem> items) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.deliveryFee = deliveryFee;
        this.items = items == null ? new ArrayList<>() : items;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public int getDeliveryFee() {
        return deliveryFee;
    }

    public List<CartItem> getItems() {
        return items;
    }
}
