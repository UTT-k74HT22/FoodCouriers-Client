package com.utt.foodcouriers_client.data.model;

import java.util.List;

public class HomeFeed {
    private final List<BannerItem> banners;
    private final List<FoodCategory> categories;
    private final List<Restaurant> featuredRestaurants;
    private final List<MenuItem> popularItems;

    public HomeFeed(
            List<BannerItem> banners,
            List<FoodCategory> categories,
            List<Restaurant> featuredRestaurants,
            List<MenuItem> popularItems
    ) {
        this.banners = banners;
        this.categories = categories;
        this.featuredRestaurants = featuredRestaurants;
        this.popularItems = popularItems;
    }

    public List<BannerItem> getBanners() {
        return banners;
    }

    public List<FoodCategory> getCategories() {
        return categories;
    }

    public List<Restaurant> getFeaturedRestaurants() {
        return featuredRestaurants;
    }

    public List<MenuItem> getPopularItems() {
        return popularItems;
    }
}
