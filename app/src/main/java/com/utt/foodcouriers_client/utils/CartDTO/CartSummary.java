package com.utt.foodcouriers_client.utils.CartDTO;

/**
 * DTO mô tả summary tổng quan của cart.
 */
public class CartSummary {
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
