package com.utt.foodcouriers_client.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Restaurant implements Serializable {
    private String id;
    private String name;
    private String description;
    private String address;
    private String phone;
    @SerializedName("image_url")
    private String imageUrl;
    private double rating;
    @SerializedName("review_count")
    private int reviewCount;
    @SerializedName("is_active")
    private boolean isActive;
    @SerializedName("is_open")
    private boolean isOpen;
    @SerializedName("open_time")
    private String openTime;
    @SerializedName("close_time")
    private String closeTime;
    @SerializedName("delivery_fee")
    private int deliveryFee;
    @SerializedName("min_order")
    private int minOrder;
    private Double latitude;
    private Double longitude;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("updated_at")
    private String updatedAt;

    public Restaurant() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isOpen() { return isOpen; }
    public void setOpen(boolean open) { isOpen = open; }

    public String getOpenTime() { return openTime; }
    public String getCloseTime() { return closeTime; }

    public int getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(int deliveryFee) { this.deliveryFee = deliveryFee; }

    public int getMinOrder() { return minOrder; }
    public void setMinOrder(int minOrder) { this.minOrder = minOrder; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    public String getFormattedRating() {
        return String.format("%.1f", rating);
    }

    public String getFormattedDeliveryFee() {
        return deliveryFee == 0 ? "Miễn phí" : deliveryFee + "k / 1km";
    }
}
