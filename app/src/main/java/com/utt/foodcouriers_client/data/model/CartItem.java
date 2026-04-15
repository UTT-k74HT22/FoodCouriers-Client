package com.utt.foodcouriers_client.data.model;

import java.io.Serializable;

public class CartItem implements Serializable {
    private String id;
    private String menuItemId;
    private String restaurantId;
    private String restaurantName;
    private String name;
    private int price;
    private int quantity;
    private String note;
    private String imageUrl;
    private Double restaurantLatitude;
    private Double restaurantLongitude;

    public CartItem() {
        this.id = null;
        this.menuItemId = null;
        this.restaurantId = null;
        this.restaurantName = null;
        this.name = null;
        this.price = 0;
        this.quantity = 0;
        this.note = null;
        this.imageUrl = null;
    }

    public CartItem(
            String id,
            String menuItemId,
            String restaurantId,
            String restaurantName,
            String name,
            int price,
            int quantity,
            String note,
            String imageUrl
    ) {
        this.id = id;
        this.menuItemId = menuItemId;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.note = note;
        this.imageUrl = imageUrl;
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

    public String getRestaurantName() {
        return restaurantName;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setMenuItem(MenuItem menuItem) {
        if (menuItem == null) {
            return;
        }

        this.menuItemId = menuItem.getId();
        this.restaurantId = menuItem.getRestaurantId();
        this.name = menuItem.getName();
        this.price = menuItem.getPrice();
        this.imageUrl = menuItem.getImageUrl();
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
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
