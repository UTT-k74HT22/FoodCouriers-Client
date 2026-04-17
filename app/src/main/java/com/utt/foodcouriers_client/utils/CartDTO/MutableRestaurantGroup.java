package com.utt.foodcouriers_client.utils.CartDTO;

import com.utt.foodcouriers_client.data.model.CartItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Model tạm dùng trong repository để gom cart item theo nhà hàng trước khi tạo
 * ra {@code CartRestaurantGroup} chính thức.
 */
public class MutableRestaurantGroup {
    private final String restaurantId;
    private final String restaurantName;
    private final int deliveryFee;
    private final List<CartItem> items = new ArrayList<>();
    private Double restaurantLatitude;
    private Double restaurantLongitude;

    public MutableRestaurantGroup(String restaurantId, String restaurantName, int deliveryFee) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.deliveryFee = deliveryFee;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public int getDeliveryFee() {
        return deliveryFee;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public Double getRestaurantLatitude() {
        return restaurantLatitude;
    }

    public void setRestaurantLatitude(Double restaurantLatitude) {
        this.restaurantLatitude = restaurantLatitude;
    }

    public Double getRestaurantLongitude() {
        return restaurantLongitude;
    }

    public void setRestaurantLongitude(Double restaurantLongitude) {
        this.restaurantLongitude = restaurantLongitude;
    }
}