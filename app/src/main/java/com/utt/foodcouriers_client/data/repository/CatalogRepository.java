package com.utt.foodcouriers_client.data.repository;

import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.BannerItem;
import com.utt.foodcouriers_client.data.model.Category;
import com.utt.foodcouriers_client.data.model.Restaurant;
import com.utt.foodcouriers_client.data.model.MenuItem;
import com.utt.foodcouriers_client.data.remote.BannerClient;
import com.utt.foodcouriers_client.data.remote.CategoryClient;
import com.utt.foodcouriers_client.data.remote.MenuItemClient;
import com.utt.foodcouriers_client.data.remote.RestaurantClient;

import java.util.List;

public class CatalogRepository {

    private static CatalogRepository instance;
    private final BannerClient bannerClient;
    private final CategoryClient categoryClient;
    private final RestaurantClient restaurantClient;
    private final MenuItemClient menuItemClient;

    public static synchronized CatalogRepository getInstance() {
        if (instance == null) {
            instance = new CatalogRepository();
        }
        return instance;
    }

    private CatalogRepository() {
        this.bannerClient = BannerClient.getInstance();
        this.categoryClient = CategoryClient.getInstance();
        this.restaurantClient = RestaurantClient.getInstance();
        this.menuItemClient = MenuItemClient.getInstance();
    }

    public void getActiveBanners(RepositoryCallback<List<BannerItem>> callback) {
        bannerClient.fetchActiveBanners(new BannerClient.ApiCallback<List<BannerItem>>() {
            @Override
            public void onSuccess(List<BannerItem> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getBannersWithTiming(RepositoryCallback<List<BannerItem>> callback) {
        bannerClient.fetchBannersWithTiming(new BannerClient.ApiCallback<List<BannerItem>>() {
            @Override
            public void onSuccess(List<BannerItem> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getActiveCategories(RepositoryCallback<List<Category>> callback) {
        categoryClient.fetchActiveCategories(new CategoryClient.ApiCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getActiveRestaurants(RepositoryCallback<List<Restaurant>> callback) {
        restaurantClient.fetchActiveRestaurants(new RestaurantClient.ApiCallback<List<Restaurant>>() {
            @Override
            public void onSuccess(List<Restaurant> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getPopularRestaurants(RepositoryCallback<List<Restaurant>> callback) {
        restaurantClient.fetchPopularRestaurants(new RestaurantClient.ApiCallback<List<Restaurant>>() {
            @Override
            public void onSuccess(List<Restaurant> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getNearbyRestaurants(RepositoryCallback<List<Restaurant>> callback) {
        restaurantClient.fetchNearbyRestaurants(new RestaurantClient.ApiCallback<List<Restaurant>>() {
            @Override
            public void onSuccess(List<Restaurant> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getPopularMenuItems(RepositoryCallback<List<MenuItem>> callback) {
        menuItemClient.fetchPopularMenuItems(new MenuItemClient.ApiCallback<List<MenuItem>>() {
            @Override
            public void onSuccess(List<MenuItem> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getAllMenuItems(RepositoryCallback<List<MenuItem>> callback) {
        menuItemClient.fetchAllMenuItems(new MenuItemClient.ApiCallback<List<MenuItem>>() {
            @Override
            public void onSuccess(List<MenuItem> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getMenuItemsByCategory(String categoryId, RepositoryCallback<List<MenuItem>> callback) {
        menuItemClient.fetchMenuItemsByCategory(categoryId, new MenuItemClient.ApiCallback<List<MenuItem>>() {
            @Override
            public void onSuccess(List<MenuItem> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getMenuItemsByRestaurant(String restaurantId, RepositoryCallback<List<MenuItem>> callback) {
        menuItemClient.fetchMenuItemsByRestaurant(restaurantId, new MenuItemClient.ApiCallback<List<MenuItem>>() {
            @Override
            public void onSuccess(List<MenuItem> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}
