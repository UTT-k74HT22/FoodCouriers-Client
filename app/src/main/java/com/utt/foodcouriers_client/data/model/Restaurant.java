package com.utt.foodcouriers_client.data.model;

import java.io.Serializable;

public class Restaurant implements Serializable {
    private final String id;
    private final String name;
    private final String category;
    private final double rating;
    private final String distance;
    private final String deliveryFee;
    private final boolean featured;
    private final String description;
    private final String openTime;
    private final String address;

    public Restaurant(
            String id,
            String name,
            String category,
            double rating,
            String distance,
            String deliveryFee,
            boolean featured,
            String description,
            String openTime,
            String address
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.rating = rating;
        this.distance = distance;
        this.deliveryFee = deliveryFee;
        this.featured = featured;
        this.description = description;
        this.openTime = openTime;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public double getRating() {
        return rating;
    }

    public String getDistance() {
        return distance;
    }

    public String getDeliveryFee() {
        return deliveryFee;
    }

    public boolean isFeatured() {
        return featured;
    }

    public String getDescription() {
        return description;
    }

    public String getOpenTime() {
        return openTime;
    }

    public String getAddress() {
        return address;
    }
}
