package com.utt.foodcouriers_client.data.model;

public class FoodCategory {
    private final String id;
    private final String name;
    private final String imageUrl;
    private final int sortOrder;

    public FoodCategory(String id, String name) {
        this(id, name, null, 0);
    }

    public FoodCategory(String id, String name, String imageUrl, int sortOrder) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
