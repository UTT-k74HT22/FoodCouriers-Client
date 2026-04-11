package com.utt.foodcouriers_client.data.model;

import com.google.gson.annotations.SerializedName;

public class BannerItem {
    private String id;
    private String title;
    @SerializedName("image_url")
    private String imageUrl;
    @SerializedName("link_type")
    private String linkType;
    @SerializedName("link_value")
    private String linkValue;
    @SerializedName("sort_order")
    private int sortOrder;
    @SerializedName("is_active")
    private boolean isActive;
    @SerializedName("start_date")
    private String startDate;
    @SerializedName("end_date")
    private String endDate;
    @SerializedName("created_at")
    private String createdAt;

    public BannerItem() {}

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setLinkType(String linkType) { this.linkType = linkType; }
    public void setLinkValue(String linkValue) { this.linkValue = linkValue; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void setActive(boolean active) { isActive = active; }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLinkType() {
        return linkType;
    }

    public String getLinkValue() {
        return linkValue;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public LinkType getLinkTypeEnum() {
        if (linkType == null) return LinkType.NONE;
        try {
            return LinkType.valueOf(linkType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LinkType.NONE;
        }
    }

    public enum LinkType {
        NONE, RESTAURANT, CATEGORY, PROMOTION, URL
    }
}
