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

    private final CategoryRepository catalogRepository = CategoryRepository.getInstance();
    private final MutableLiveData<List<FoodCategory>> categoryFilters = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<FoodCategory>> spotlightCategories = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> selectedCategoryId = new MutableLiveData<>(null);
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
        String currentId = selectedCategoryId.getValue();
        String nextId = category.getId();
        selectedCategoryId.setValue(nextId != null && nextId.equals(currentId) ? null : nextId);
        publishState();
    }

    private void publishState() {
        categoryFilters.setValue(new ArrayList<>(sourceCategories));

        String selectedId = selectedCategoryId.getValue();
        final String currentSelectedId = selectedId;
        if (currentSelectedId != null && sourceCategories.stream().noneMatch(category -> currentSelectedId.equals(category.getId()))) {
            selectedId = null;
            selectedCategoryId.setValue(null);
        }
        final String finalSelectedId = selectedId;

        List<FoodCategory> ordered = new ArrayList<>(sourceCategories);
        if (finalSelectedId != null) {
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
