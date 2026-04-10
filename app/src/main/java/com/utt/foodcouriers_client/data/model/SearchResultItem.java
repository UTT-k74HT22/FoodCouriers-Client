package com.utt.foodcouriers_client.data.model;

public class SearchResultItem {
    public enum Type {
        RESTAURANT,
        MENU_ITEM
    }

    private final Type type;
    private final Restaurant restaurant;
    private final MenuItem menuItem;

    private SearchResultItem(Type type, Restaurant restaurant, MenuItem menuItem) {
        this.type = type;
        this.restaurant = restaurant;
        this.menuItem = menuItem;
    }

    public static SearchResultItem fromRestaurant(Restaurant restaurant) {
        return new SearchResultItem(Type.RESTAURANT, restaurant, null);
    }

    public static SearchResultItem fromMenuItem(MenuItem menuItem) {
        return new SearchResultItem(Type.MENU_ITEM, null, menuItem);
    }

    public Type getType() {
        return type;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }
}
