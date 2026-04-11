package com.utt.foodcouriers_client.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.FoodCategory;
import com.utt.foodcouriers_client.data.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeViewModel extends BaseViewModel {

    private static final String ALL_CATEGORY_ID = "all";

    private final CategoryRepository catalogRepository = CategoryRepository.getInstance();
    private final MutableLiveData<List<FoodCategory>> categoryFilters = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<FoodCategory>> spotlightCategories = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> selectedCategoryId = new MutableLiveData<>(ALL_CATEGORY_ID);
    private List<FoodCategory> sourceCategories = Collections.emptyList();

    public HomeViewModel() {
        refreshCategories();
    }

    public LiveData<List<FoodCategory>> getCategoryFilters() {
        return categoryFilters;
    }

    public LiveData<List<FoodCategory>> getSpotlightCategories() {
        return spotlightCategories;
    }

    public LiveData<String> getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void refreshCategories() {
        setLoading(true);
        catalogRepository.getCategories(new RepositoryCallback<List<FoodCategory>>() {
            @Override
            public void onSuccess(List<FoodCategory> result) {
                sourceCategories = result != null ? result : Collections.emptyList();
                setLoading(false);
                errorMessage.setValue(null);
                publishState();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                postError(error);
            }
        });
    }

    public void selectCategory(FoodCategory category) {
        if (category == null) {
            return;
        }
        selectedCategoryId.setValue(category.getId());
        publishState();
    }

    private void publishState() {
        List<FoodCategory> filters = new ArrayList<>();
        filters.addAll(sourceCategories);
        categoryFilters.setValue(filters);

        String selectedId = selectedCategoryId.getValue();
        if (selectedId == null) {
            selectedId = ALL_CATEGORY_ID;
            selectedCategoryId.setValue(selectedId);
        }
        final String finalSelectedId = selectedId;

        List<FoodCategory> ordered = new ArrayList<>(sourceCategories);
        if (!ALL_CATEGORY_ID.equals(finalSelectedId)) {
            ordered.sort((left, right) -> {
                boolean leftSelected = finalSelectedId.equals(left.getId());
                boolean rightSelected = finalSelectedId.equals(right.getId());
                if (leftSelected == rightSelected) {
                    return Integer.compare(left.getSortOrder(), right.getSortOrder());
                }
                return leftSelected ? -1 : 1;
            });
        }
        spotlightCategories.setValue(ordered);
    }
}
