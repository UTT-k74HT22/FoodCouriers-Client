package com.utt.foodcouriers_client.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.BannerItem;
import com.utt.foodcouriers_client.data.model.Category;
import com.utt.foodcouriers_client.data.model.MenuItem;
import com.utt.foodcouriers_client.data.model.Restaurant;
import com.utt.foodcouriers_client.data.repository.CatalogRepository;

import java.util.List;

public class HomeViewModel extends BaseViewModel {

    private final MutableLiveData<List<BannerItem>> banners = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<Restaurant>> popularRestaurants = new MutableLiveData<>();
    private final MutableLiveData<List<Restaurant>> nearbyRestaurants = new MutableLiveData<>();
    private final MutableLiveData<List<MenuItem>> popularMenuItems = new MutableLiveData<>();
    private final MutableLiveData<String> selectedCategoryId = new MutableLiveData<>();
    private final CatalogRepository catalogRepository;

    public HomeViewModel() {
        this.catalogRepository = CatalogRepository.getInstance();
    }

    public MutableLiveData<List<BannerItem>> getBanners() {
        return banners;
    }

    public MutableLiveData<List<Category>> getCategories() {
        return categories;
    }

    public MutableLiveData<List<Restaurant>> getPopularRestaurants() {
        return popularRestaurants;
    }

    public MutableLiveData<List<Restaurant>> getNearbyRestaurants() {
        return nearbyRestaurants;
    }

    public MutableLiveData<String> getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public MutableLiveData<List<MenuItem>> getPopularMenuItems() {
        return popularMenuItems;
    }

    public void loadBanners() {
        setLoading(true);
        errorMessage.setValue(null);
        
        catalogRepository.getBannersWithTiming(new RepositoryCallback<List<BannerItem>>() {
            @Override
            public void onSuccess(List<BannerItem> result) {
                setLoading(false);
                banners.postValue(result);
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                postError(error);
            }
        });
    }

    public void loadCategories() {
        catalogRepository.getActiveCategories(new RepositoryCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                categories.postValue(result);
            }

            @Override
            public void onError(String error) {
                postError(error);
            }
        });
    }

    public void loadPopularRestaurants() {
        catalogRepository.getPopularRestaurants(new RepositoryCallback<List<Restaurant>>() {
            @Override
            public void onSuccess(List<Restaurant> result) {
                popularRestaurants.postValue(result);
            }

            @Override
            public void onError(String error) {
                postError(error);
            }
        });
    }

    public void loadNearbyRestaurants() {
        catalogRepository.getNearbyRestaurants(new RepositoryCallback<List<Restaurant>>() {
            @Override
            public void onSuccess(List<Restaurant> result) {
                nearbyRestaurants.postValue(result);
            }

            @Override
            public void onError(String error) {
                postError(error);
            }
        });
    }

    public void loadPopularMenuItems() {
        catalogRepository.getPopularMenuItems(new RepositoryCallback<List<MenuItem>>() {
            @Override
            public void onSuccess(List<MenuItem> result) {
                popularMenuItems.postValue(result);
            }

            @Override
            public void onError(String error) {
                postError(error);
            }
        });
    }

    public void selectCategory(Category category) {
        selectedCategoryId.setValue(category != null ? category.getId() : null);
    }

    public void filterMenuItemsByCategory(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) {
            loadPopularMenuItems();
            return;
        }
        
        catalogRepository.getMenuItemsByCategory(categoryId, new RepositoryCallback<List<MenuItem>>() {
            @Override
            public void onSuccess(List<MenuItem> result) {
                popularMenuItems.postValue(result);
            }

            @Override
            public void onError(String error) {
                postError(error);
            }
        });
    }

    public void refreshBanners() {
        loadBanners();
    }
}
