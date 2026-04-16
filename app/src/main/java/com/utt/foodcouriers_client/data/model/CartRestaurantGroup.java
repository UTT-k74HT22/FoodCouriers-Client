package com.utt.foodcouriers_client.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CartRestaurantGroup implements Serializable {

    private final String restaurantId;
    private final String restaurantName;
    private final int deliveryFee;
    private final int calculatedDeliveryFee;
    private final List<CartItem> items;
    private final Double restaurantLatitude;
    private final Double restaurantLongitude;

    public CartRestaurantGroup(String restaurantId, String restaurantName, int deliveryFee, List<CartItem> items) {
        this(restaurantId, restaurantName, deliveryFee, 0, items, null, null);
    }

    public CartRestaurantGroup(String restaurantId, String restaurantName, int deliveryFee, int calculatedDeliveryFee, List<CartItem> items,
                                Double restaurantLatitude, Double restaurantLongitude) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.deliveryFee = deliveryFee;
        this.calculatedDeliveryFee = calculatedDeliveryFee;
        this.items = items == null ? new ArrayList<>() : items;
        this.restaurantLatitude = restaurantLatitude;
        this.restaurantLongitude = restaurantLongitude;
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

    public int getCalculatedDeliveryFee() {
        return calculatedDeliveryFee;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public Double getRestaurantLatitude() {
        return restaurantLatitude;
    }

    public Double getRestaurantLongitude() {
        return restaurantLongitude;
    }
}
