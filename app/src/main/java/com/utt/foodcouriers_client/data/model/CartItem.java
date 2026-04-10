package com.utt.foodcouriers_client.data.model;

import java.io.Serializable;

public class CartItem implements Serializable {
    private final String id;
    private final String menuItemId;
    private final String restaurantId;
    private final String name;
    private final int price;
    private int quantity;
    private final String note;

    public CartItem(String id, String menuItemId, String restaurantId, String name, int price, int quantity, String note) {
        this.id = id;
        this.menuItemId = menuItemId;
        this.restaurantId = restaurantId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public String getMenuItemId() {
        return menuItemId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getNote() {
        return note;
    }
}
