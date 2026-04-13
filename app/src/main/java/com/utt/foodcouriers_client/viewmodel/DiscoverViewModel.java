package com.utt.foodcouriers_client.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.MenuItem;
import com.utt.foodcouriers_client.data.model.Restaurant;
import com.utt.foodcouriers_client.data.repository.CatalogRepository;
import com.utt.foodcouriers_client.ui.discover.adapter.RestaurantWithMenuAdapter;

import java.util.ArrayList;
import java.util.List;

public class DiscoverViewModel extends BaseViewModel {

    private final MutableLiveData<List<RestaurantWithMenuAdapter.RestaurantWithMenu>> restaurantsWithMenus = new MutableLiveData<>();
    private final CatalogRepository catalogRepository;
    private String currentSearchQuery = "";
    private List<RestaurantWithMenuAdapter.RestaurantWithMenu> allRestaurantsWithMenus = new ArrayList<>();

    public DiscoverViewModel() {
        this.catalogRepository = CatalogRepository.getInstance();
    }

    public MutableLiveData<List<RestaurantWithMenuAdapter.RestaurantWithMenu>> getRestaurantsWithMenus() {
        return restaurantsWithMenus;
    }

    public void search(String query) {
        currentSearchQuery = query != null ? query.trim().toLowerCase() : "";
        filterRestaurants();
    }

    private void filterRestaurants() {
        if (allRestaurantsWithMenus.isEmpty()) {
            restaurantsWithMenus.postValue(new ArrayList<>());
            return;
        }

        if (currentSearchQuery.isEmpty()) {
            restaurantsWithMenus.postValue(new ArrayList<>(allRestaurantsWithMenus));
            return;
        }

        List<RestaurantWithMenuAdapter.RestaurantWithMenu> filtered = new ArrayList<>();
        for (RestaurantWithMenuAdapter.RestaurantWithMenu item : allRestaurantsWithMenus) {
            List<MenuItem> matchingMenuItems = new ArrayList<>();
            if (item.menuItems != null) {
                for (MenuItem menuItem : item.menuItems) {
                    if (menuItem.getName() != null && 
                        menuItem.getName().toLowerCase().contains(currentSearchQuery)) {
                        matchingMenuItems.add(menuItem);
                    }
                }
            }

            if (!matchingMenuItems.isEmpty()) {
                filtered.add(new RestaurantWithMenuAdapter.RestaurantWithMenu(
                    item.restaurant, matchingMenuItems));
            }
        }

        restaurantsWithMenus.postValue(filtered);
    }

    public void loadRestaurantsWithMenus() {
        setLoading(true);
        errorMessage.setValue(null);

        catalogRepository.getActiveRestaurants(new RepositoryCallback<List<Restaurant>>() {
            @Override
            public void onSuccess(List<Restaurant> restaurants) {
                if (restaurants == null || restaurants.isEmpty()) {
                    setLoading(false);
                    restaurantsWithMenus.postValue(new ArrayList<>());
                    return;
                }

                final List<RestaurantWithMenuAdapter.RestaurantWithMenu> result = new ArrayList<>();
                final int[] pending = {restaurants.size()};

                for (Restaurant restaurant : restaurants) {
                    catalogRepository.getMenuItemsByRestaurant(restaurant.getId(), new RepositoryCallback<List<MenuItem>>() {
@Override
public void onSuccess(List<MenuItem> menuItems) {
    synchronized (result) {
        result.add(new RestaurantWithMenuAdapter.RestaurantWithMenu(restaurant, menuItems));
        pending[0]--;
        if (pending[0] == 0) {
            setLoading(false);
            allRestaurantsWithMenus = new ArrayList<>(result);
            filterRestaurants();
        }
    }
}

@Override
public void onError(String error) {
    synchronized (result) {
        result.add(new RestaurantWithMenuAdapter.RestaurantWithMenu(restaurant, new ArrayList<>()));
        pending[0]--;
        if (pending[0] == 0) {
            setLoading(false);
            allRestaurantsWithMenus = new ArrayList<>(result);
            filterRestaurants();
        }
    }
}
                    });
                }
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                postError(error);
            }
        });
    }
}