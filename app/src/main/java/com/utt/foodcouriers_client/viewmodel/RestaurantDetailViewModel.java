package com.utt.foodcouriers_client.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.utt.foodcouriers_client.data.model.MenuItem;
import com.utt.foodcouriers_client.data.model.Restaurant;
import com.utt.foodcouriers_client.data.remote.BaseSupabaseClient.ApiCallback;
import com.utt.foodcouriers_client.data.remote.MenuItemClient;
import com.utt.foodcouriers_client.data.remote.RestaurantClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestaurantDetailViewModel extends BaseViewModel {

    private final MutableLiveData<Restaurant> restaurant = new MutableLiveData<>();
    private final MutableLiveData<Map<String, List<MenuItem>>> menuByCategory = new MutableLiveData<>();
    private final MutableLiveData<String> selectedMenuItemId = new MutableLiveData<>();

    public LiveData<Restaurant> getRestaurant() {
        return restaurant;
    }

    public LiveData<Map<String, List<MenuItem>>> getMenuByCategory() {
        return menuByCategory;
    }

    public LiveData<String> getSelectedMenuItemId() {
        return selectedMenuItemId;
    }

    public void loadRestaurantDetail(String restaurantId, String menuItemId) {
        loading.setValue(true);
        errorMessage.setValue(null);

        RestaurantClient.getInstance().fetchRestaurantsById(restaurantId, new ApiCallback<Restaurant>() {
            @Override
            public void onSuccess(Restaurant result) {
                restaurant.postValue(result);
                if (menuItemId != null && !menuItemId.isEmpty()) {
                    selectedMenuItemId.postValue(menuItemId);
                }
                loadMenuItems(restaurantId);
            }

            @Override
            public void onError(String error) {
                loading.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }

    private void loadMenuItems(String restaurantId) {
        MenuItemClient.getInstance().fetchMenuItemsByRestaurant(restaurantId, new ApiCallback<List<MenuItem>>() {
            @Override
            public void onSuccess(List<MenuItem> result) {
                loading.postValue(false);
                Map<String, List<MenuItem>> grouped = groupMenuByCategory(result);
                menuByCategory.postValue(grouped);
            }

            @Override
            public void onError(String error) {
                loading.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }

    private Map<String, List<MenuItem>> groupMenuByCategory(List<MenuItem> items) {
        Map<String, List<MenuItem>> grouped = new HashMap<>();
        if (items == null || items.isEmpty()) {
            return grouped;
        }

        for (MenuItem item : items) {
            String category = item.getCategory() != null ? item.getCategory() : "Other";
            if (!grouped.containsKey(category)) {
                grouped.put(category, new ArrayList<>());
            }
            grouped.get(category).add(item);
        }
        return grouped;
    }
}