package com.utt.foodcouriers_client.data.model;

import java.io.Serializable;

public class MenuItem implements Serializable {
    private final String id;
    private final String restaurantId;
    private final String category;
    private final String name;
    private final String description;
    private final int price;
    private final double rating;

    public MenuItem(String id, String restaurantId, String category, String name, String description, int price, double rating) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.rating = rating;
    }

    public String getId() {
        return id;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public double getRating() {
        return rating;
    }
}
