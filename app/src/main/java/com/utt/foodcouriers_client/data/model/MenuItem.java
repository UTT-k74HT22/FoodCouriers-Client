package com.utt.foodcouriers_client.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class MenuItem implements Serializable {
    private String id;
    @SerializedName("restaurant_id")
    private String restaurantId;
    @SerializedName("category_id")
    private String categoryId;
    private String category;
    private String name;
    private String description;
    private int price;
    @SerializedName("image_url")
    private String imageUrl;
    private double rating;
    @SerializedName("is_featured")
    private boolean isFeatured;
    @SerializedName("is_available")
    private boolean isAvailable;

    public MenuItem() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public boolean isFeatured() { return isFeatured; }
    public void setFeatured(boolean featured) { isFeatured = featured; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }
}