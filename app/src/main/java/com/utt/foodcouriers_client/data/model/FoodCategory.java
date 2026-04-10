package com.utt.foodcouriers_client.data.model;

public class FoodCategory {
    private final String id;
    private final String name;

    public FoodCategory(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
